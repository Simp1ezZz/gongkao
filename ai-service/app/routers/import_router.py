"""Paper import endpoints: upload files and parse them."""

import os
import uuid
from fastapi import APIRouter, UploadFile, File, HTTPException
from pydantic import BaseModel

from app.services.html_parser import parse_all, parse_questions_file
from app.services.image_handler import (
    download_and_upload_images,
    replace_urls_in_html,
    get_minio_client,
)

router = APIRouter()

TEMP_DIR = os.path.join(os.environ.get("TEMP", "/tmp"), "gongkao_import")
os.makedirs(TEMP_DIR, exist_ok=True)


class ParseRequest(BaseModel):
    temp_id: str


def read_file(path: str) -> str:
    for enc in ("utf-8-sig", "utf-8", "gbk"):
        try:
            with open(path, "r", encoding=enc) as f:
                return f.read()
        except (UnicodeDecodeError, ValueError):
            continue
    raise ValueError(f"Cannot decode file: {path}")


@router.post("/import/upload")
async def upload_files(
    questions: UploadFile = File(...),
    answers: UploadFile = File(...),
    explanations: UploadFile = File(...),
):
    temp_id = str(uuid.uuid4())
    dir_path = os.path.join(TEMP_DIR, temp_id)
    os.makedirs(dir_path, exist_ok=True)

    for name, uploaded in [("questions", questions), ("answers", answers), ("explanations", explanations)]:
        file_path = os.path.join(dir_path, name)
        content = await uploaded.read()
        with open(file_path, "wb") as f:
            f.write(content)

    try:
        q_html = read_file(os.path.join(dir_path, "questions"))
        metadata, _, _ = parse_questions_file(q_html)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to parse files: {e}")

    return {
        "temp_id": temp_id,
        "metadata": {
            "title": metadata.title,
            "year": metadata.year,
            "category": metadata.category,
            "region_name": metadata.region_name,
        },
    }


@router.post("/import/parse")
async def parse_files(req: ParseRequest):
    dir_path = os.path.join(TEMP_DIR, req.temp_id)
    if not os.path.isdir(dir_path):
        raise HTTPException(status_code=404, detail="Temp files not found. Please upload again.")

    try:
        q_html = read_file(os.path.join(dir_path, "questions"))
        a_html = read_file(os.path.join(dir_path, "answers"))
        e_html = read_file(os.path.join(dir_path, "explanations"))
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to read files: {e}")

    try:
        paper = parse_all(q_html, a_html, e_html)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to parse paper: {e}")

    # Collect all image URLs
    all_image_urls = []
    for section in paper.sections:
        for q in section.questions:
            all_image_urls.extend(q.images)
            for opt in q.options:
                if opt.image:
                    all_image_urls.append(opt.image)
    for mg in paper.material_groups:
        all_image_urls.extend(mg.images)

    unique_urls = list(set(url for url in all_image_urls if url.startswith("http")))

    # Download and upload images to MinIO
    url_map = {}
    if unique_urls:
        try:
            minio_client = get_minio_client()
            url_map = await download_and_upload_images(unique_urls, minio_client)
        except Exception:
            pass

    # Replace URLs in content
    for section in paper.sections:
        for q in section.questions:
            q.content = replace_urls_in_html(q.content, url_map)
            q.explanation = replace_urls_in_html(q.explanation, url_map)
            for opt in q.options:
                opt.text = replace_urls_in_html(opt.text, url_map)
                if opt.image and opt.image in url_map:
                    opt.image = f"/api/files/serve/{url_map[opt.image]}"
            q.images = [f"/api/files/serve/{url_map[u]}" for u in q.images if u in url_map]

    for mg in paper.material_groups:
        mg.content = replace_urls_in_html(mg.content, url_map)
        mg.images = [f"/api/files/serve/{url_map[u]}" for u in mg.images if u in url_map]

    return paper.model_dump(by_alias=True)
