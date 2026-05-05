import io
import re
import zipfile
import logging
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field

import fitz  # PyMuPDF

from app.services.minio_client import upload_image, get_presigned_url

logger = logging.getLogger(__name__)

NS_W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
NS_WP = "http://schemas.openxmlformats.org/drawingml/2006/main"
NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"


@dataclass
class ExtractedImage:
    key: str          # MinIO object key
    url: str          # presigned URL
    placeholder: str  # e.g. [img_0]


@dataclass
class ParseResult:
    text: str                       # full extracted text with [img_N] placeholders
    images: list[ExtractedImage] = field(default_factory=list)


def parse_pdf(file_bytes: bytes, session_id: str) -> ParseResult:
    doc = fitz.open(stream=file_bytes, filetype="pdf")
    all_text_parts: list[str] = []
    all_images: list[ExtractedImage] = []
    img_counter = 0

    for page_num in range(doc.page_count):
        page = doc[page_num]
        # Get text blocks with position info
        blocks = page.get_text("dict")["blocks"]
        for block in blocks:
            if block["type"] == 0:  # text block
                for line in block["lines"]:
                    line_text = ""
                    for span in line["spans"]:
                        line_text += span["text"]
                    all_text_parts.append(line_text)
            elif block["type"] == 1:  # image block
                placeholder = f"[img_{img_counter}]"
                all_text_parts.append(placeholder)
                try:
                    img_data = block["image"]
                    if isinstance(img_data, bytes):
                        pix = fitz.Pixmap(img_data)
                    else:
                        pix = fitz.Pixmap(doc, img_data)
                    if pix.n >= 5:  # CMYK → convert
                        pix = fitz.Pixmap(fitz.csRGB, pix)
                    img_bytes = pix.tobytes("png")
                    key = upload_image(img_bytes, f"p{page_num}_b{img_counter}.png", session_id)
                    url = get_presigned_url(key)
                    all_images.append(ExtractedImage(key=key, url=url, placeholder=placeholder))
                except Exception as e:
                    logger.warning("Failed to extract image block on page %d: %s", page_num, e)
                    all_text_parts.append("[图片提取失败]")
                img_counter += 1

        # Also extract embedded images not in blocks
        for img_info in page.get_images():
            xref = img_info[0]
            # Skip if we already extracted this image via block
            if any(im.key.endswith(f"_xref{xref}.png") for im in all_images):
                continue
            try:
                pix = fitz.Pixmap(doc, xref)
                if pix.n >= 5:
                    pix = fitz.Pixmap(fitz.csRGB, pix)
                if pix.width < 50 or pix.height < 50:
                    continue  # skip tiny images (icons, bullets)
                img_bytes = pix.tobytes("png")
                placeholder = f"[img_{img_counter}]"
                key = upload_image(img_bytes, f"p{page_num}_xref{xref}.png", session_id)
                url = get_presigned_url(key)
                all_images.append(ExtractedImage(key=key, url=url, placeholder=placeholder))
                img_counter += 1
            except Exception as e:
                logger.warning("Failed to extract embedded image xref=%d: %s", xref, e)

    text = "\n".join(all_text_parts)
    return ParseResult(text=text, images=all_images)


def parse_docx(file_bytes: bytes, session_id: str) -> ParseResult:
    all_text_parts: list[str] = []
    all_images: list[ExtractedImage] = []
    img_counter = 0

    # Fallback: parse XML directly from ZIP (handles malformed docx)
    with zipfile.ZipFile(io.BytesIO(file_bytes)) as zf:
        names = zf.namelist()

        # Build image mapping: filename -> image bytes
        image_map: dict[str, bytes] = {}
        for name in names:
            if name.startswith("word/media/"):
                image_map[name.split("/")[-1]] = zf.read(name)

        # Parse relationships to map rId -> media file
        rel_map: dict[str, str] = {}
        if "word/_rels/document.xml.rels" in names:
            rels_xml = zf.read("word/_rels/document.xml.rels")
            rel_root = ET.fromstring(rels_xml)
            for rel in rel_root:
                rid = rel.get("Id", "")
                target = rel.get("Target", "")
                if target.startswith("media/"):
                    rel_map[rid] = target.replace("media/", "")

        # Parse document.xml
        doc_xml = zf.read("word/document.xml")
        root = ET.fromstring(doc_xml)

        for para in root.iter(f"{{{NS_W}}}p"):
            para_texts: list[str] = []

            for run in para.iter(f"{{{NS_W}}}r"):
                # Check for drawing/image
                drawing = run.find(f".//{{{NS_W}}}drawing")
                if drawing is None:
                    drawing = run.find(f".//{{{NS_W}}}pict")

                if drawing is not None:
                    placeholder = f"[img_{img_counter}]"
                    para_texts.append(placeholder)

                    # Try to find the image relationship
                    blip = drawing.find(f".//{{{NS_WP}}}blip")
                    if blip is not None:
                        embed_id = blip.get(f"{{{NS_REL}}}embed", "")
                        img_name = rel_map.get(embed_id, "")
                        if img_name and img_name in image_map:
                            try:
                                img_bytes = image_map[img_name]
                                key = upload_image(img_bytes, img_name, session_id)
                                url = get_presigned_url(key)
                                all_images.append(
                                    ExtractedImage(key=key, url=url, placeholder=placeholder)
                                )
                            except Exception as e:
                                logger.warning("Failed to upload image %s: %s", img_name, e)
                                para_texts[-1] = "[图片提取失败]"
                    img_counter += 1

                # Extract text
                for t in run.iter(f"{{{NS_W}}}t"):
                    if t.text:
                        para_texts.append(t.text)

            line = "".join(para_texts).strip()
            if line:
                all_text_parts.append(line)

    text = "\n".join(all_text_parts)
    return ParseResult(text=text, images=all_images)


def parse_file(file_bytes: bytes, filename: str, session_id: str) -> ParseResult:
    ext = filename.rsplit(".", 1)[-1].lower()
    if ext == "pdf":
        return parse_pdf(file_bytes, session_id)
    elif ext in ("docx", "doc"):
        return parse_docx(file_bytes, session_id)
    else:
        raise ValueError(f"Unsupported file format: {ext}")
