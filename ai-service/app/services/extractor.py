import json
import logging
import re
from dataclasses import dataclass, field

from app.services.parser import parse_file, ParseResult
from app.services.module_detector import split_text_by_modules
from app.services.llm_client import call_llm

logger = logging.getLogger(__name__)


@dataclass
class ParsedQuestion:
    sort_order: int
    module: str
    sub_module: str | None
    type: str
    content: str
    options: list[dict]
    answer: str
    explanation: str
    score: float
    images: list[str] = field(default_factory=list)
    material_group_index: int | None = None


@dataclass
class ParsedMaterialGroup:
    sort_order: int
    title: str
    content: str
    images: list[str] = field(default_factory=list)


@dataclass
class ExtractionResult:
    questions: list[ParsedQuestion]
    material_groups: list[ParsedMaterialGroup]
    stats: dict


def _match_answers(questions_text: str, answers_text: str) -> dict[int, str]:
    """Extract per-question answer text from the answer file.

    Returns a dict of question_number -> answer_text.
    """
    result: dict[int, str] = {}
    # Split by "题目N解析：" pattern
    parts = re.split(r"题目\s*(\d+)\s*解析[：:]", answers_text)
    # parts: [preamble, num1, text1, num2, text2, ...]
    for i in range(1, len(parts), 2):
        qnum = int(parts[i])
        text = parts[i + 1] if i + 1 < len(parts) else ""
        result[qnum] = text.strip()
    return result


def _extract_correct_answer(answer_text: str) -> str:
    """Extract the correct answer letter from answer text like '故正确答案为C'."""
    m = re.search(r"故正确答案为\s*([A-Z]+)", answer_text)
    if m:
        return m.group(1)
    return ""


def _replace_image_placeholders(html: str, image_map: dict[str, str]) -> str:
    """Replace [img_N] placeholders with <img> tags."""
    for placeholder, url in image_map.items():
        html = html.replace(placeholder, f'<img src="{url}" alt="题目图片">')
    return html


async def extract_questions(
    question_file_bytes: bytes,
    question_filename: str,
    answer_file_bytes: bytes,
    answer_filename: str,
    session_id: str,
    model_name: str | None = None,
    progress_callback=None,
) -> ExtractionResult:
    """Main extraction pipeline.

    Args:
        progress_callback: async callable(module_name, status, count)
            status: "parsing" | "extracting" | "done" | "failed"
    """
    # 1. Parse both files
    if progress_callback:
        await progress_callback("全部", "parsing", 0)

    q_result: ParseResult = parse_file(question_file_bytes, question_filename, session_id)
    a_result: ParseResult = parse_file(answer_file_bytes, answer_filename, session_id)

    # Build image map: placeholder -> presigned URL
    image_map = {img.placeholder: img.url for img in q_result.images}
    image_map.update({img.placeholder: img.url for img in a_result.images})

    # 2. Split question text by modules
    module_texts = split_text_by_modules(q_result.text)
    logger.warning("Detected %d modules: %s", len(module_texts), list(module_texts.keys()))

    # 3. Match answers to question numbers
    answer_map = _match_answers(q_result.text, a_result.text)
    logger.warning("Matched answers for %d questions", len(answer_map))

    # 4. For each module, call LLM
    all_questions: list[ParsedQuestion] = []
    all_material_groups: list[ParsedMaterialGroup] = []
    module_stats: dict[str, int] = {}

    for module_name, module_text in module_texts.items():
        if not module_text.strip():
            continue

        logger.warning("Processing module '%s' (%d chars)", module_name, len(module_text))

        # Build answer text for this module's questions
        q_nums = re.findall(r"(?:^|\n)\s*(\d{1,3})\s*[\.。．]\s*\n", module_text)
        module_answer_parts: list[str] = []
        for qn in q_nums:
            num = int(qn)
            if num in answer_map:
                module_answer_parts.append(f"题目{num}解析：\n{answer_map[num]}")
        module_answer_text = "\n\n".join(module_answer_parts) if module_answer_parts else a_result.text

        if progress_callback:
            await progress_callback(module_name, "extracting", 0)

        has_images = any(img.placeholder in module_text for img in q_result.images)

        try:
            result = await call_llm(model_name, module_text, module_answer_text, has_images)
            logger.info("Module '%s' extracted %d questions", module_name, len(result.get("questions", [])))
        except Exception as e:
            logger.error("LLM extraction failed for module %s: %s", module_name, e)
            if progress_callback:
                await progress_callback(module_name, "failed", 0)
            continue

        # Process material_groups
        if "material_groups" in result:
            for mg_data in result["material_groups"]:
                mg = ParsedMaterialGroup(
                    sort_order=mg_data.get("sort_order", len(all_material_groups) + 1),
                    title=mg_data.get("title", ""),
                    content=_replace_image_placeholders(
                        mg_data.get("content", ""), image_map
                    ),
                    images=[img for img in image_map.values()
                            if img in mg_data.get("content", "")],
                )
                all_material_groups.append(mg)

        # Process questions
        if "questions" in result:
            for q_data in result["questions"]:
                q = ParsedQuestion(
                    sort_order=q_data.get("sort_order", 0),
                    module=q_data.get("module", module_name),
                    sub_module=q_data.get("sub_module"),
                    type=q_data.get("type", "single_choice"),
                    content=_replace_image_placeholders(
                        q_data.get("content", ""), image_map
                    ),
                    options=q_data.get("options", []),
                    answer=_extract_correct_answer(
                        q_data.get("explanation", "")
                    ) or q_data.get("answer", ""),
                    explanation=_replace_image_placeholders(
                        q_data.get("explanation", ""), image_map
                    ),
                    score=q_data.get("score", 0.8),
                    material_group_index=q_data.get("material_group_index"),
                )
                all_questions.append(q)

        module_stats[module_name] = len(result.get("questions", []))
        if progress_callback:
            await progress_callback(module_name, "done", len(result.get("questions", [])))

    # Sort by sort_order
    all_questions.sort(key=lambda q: q.sort_order)

    stats = {
        "total_questions": len(all_questions),
        "total_material_groups": len(all_material_groups),
        "modules": module_stats,
    }

    return ExtractionResult(
        questions=all_questions,
        material_groups=all_material_groups,
        stats=stats,
    )
