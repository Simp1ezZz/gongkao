import uuid
import logging
from fastapi import APIRouter, UploadFile, File, Form, HTTPException
from typing import Optional

from app.services.extractor import extract_questions
from app.services.llm_client import get_available_models

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/import", tags=["import"])


@router.get("/models")
async def list_models():
    models = await get_available_models()
    return {"models": models}


@router.post("/parse")
async def parse_files(
    question_file: UploadFile = File(...),
    answer_file: UploadFile = File(...),
    paper_title: str = Form(...),
    paper_year: int = Form(...),
    paper_category: str = Form("行测"),
    region_id: Optional[int] = Form(None),
    model_name: Optional[str] = Form(None),
):
    # Validate file types
    q_ext = (question_file.filename or "").rsplit(".", 1)[-1].lower()
    a_ext = (answer_file.filename or "").rsplit(".", 1)[-1].lower()
    if q_ext not in ("pdf",):
        raise HTTPException(400, "题目文件仅支持PDF格式")
    if a_ext not in ("pdf", "docx", "doc"):
        raise HTTPException(400, "答案文件支持PDF或DOCX格式")

    # Read file bytes
    try:
        q_bytes = await question_file.read()
        a_bytes = await answer_file.read()
    except Exception as e:
        raise HTTPException(400, f"文件读取失败: {e}")

    if len(q_bytes) > 50 * 1024 * 1024 or len(a_bytes) > 50 * 1024 * 1024:
        raise HTTPException(400, "文件大小不能超过50MB")

    session_id = uuid.uuid4().hex

    try:
        result = await extract_questions(
            question_file_bytes=q_bytes,
            question_filename=question_file.filename or "questions.pdf",
            answer_file_bytes=a_bytes,
            answer_filename=answer_file.filename or "answers.docx",
            session_id=session_id,
            model_name=model_name,
        )
    except Exception as e:
        logger.exception("Extraction failed")
        raise HTTPException(500, f"解析失败: {e}")

    # Serialize result
    questions_data = []
    for q in result.questions:
        questions_data.append({
            "sort_order": q.sort_order,
            "module": q.module,
            "sub_module": q.sub_module,
            "type": q.type,
            "content": q.content,
            "options": q.options,
            "answer": q.answer,
            "explanation": q.explanation,
            "score": q.score,
            "material_group_index": q.material_group_index,
        })

    material_groups_data = []
    for mg in result.material_groups:
        material_groups_data.append({
            "sort_order": mg.sort_order,
            "title": mg.title,
            "content": mg.content,
            "images": mg.images,
        })

    return {
        "parse_id": session_id,
        "status": "completed",
        "paper": {
            "title": paper_title,
            "year": paper_year,
            "category": paper_category,
            "region_id": region_id,
        },
        "questions": questions_data,
        "material_groups": material_groups_data,
        "stats": result.stats,
    }


@router.post("/reparse-question")
async def reparse_question(
    question_text: str = Form(...),
    answer_text: str = Form(""),
    module: str = Form("未分类"),
    model_name: Optional[str] = Form(None),
):
    """Re-parse a single question using LLM."""
    from app.services.llm_client import call_llm

    try:
        result = await call_llm(model_name, question_text, answer_text)
        return {"question": result.get("questions", [{}])[0] if result.get("questions") else {}}
    except Exception as e:
        logger.exception("Re-parse failed")
        raise HTTPException(500, f"重新解析失败: {e}")
