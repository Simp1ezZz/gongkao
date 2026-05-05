import json
import logging
import httpx
from app.core.config import settings, ModelConfig

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """你是公考题目结构化提取专家。你需要将原始试卷文本和答案解析转换为结构化JSON数据。

## 输出格式
严格返回一个JSON对象，包含 "questions" 数组和可选的 "material_groups" 数组。

每个question包含：
- sort_order: 题号（整数）
- module: 从以下选择：政治理论/常识判断/言语理解与表达/数量关系/判断推理/资料分析
- sub_module: 根据题型判断，如：片段阅读/逻辑填空/图形推理/定义判断/类比推理/逻辑判断/数学运算。无法判断则设为null
- type: 题目类型，单选"single_choice"，多选"multi_choice"
- content: 题目正文（HTML格式，用<p>标签包裹段落，图片保持[img_N]标记）
- options: 选项数组，每个含label(A/B/C/D)和text
- answer: 正确答案字母，如"A"或"AB"
- explanation: 解析内容（HTML格式）
- score: 分值（默认0.8）

每个material_group包含：
- sort_order: 材料序号
- title: 材料标题
- content: 材料正文（HTML格式）

## 规则
1. 图片标记保持为 [img_N] 格式，不要省略
2. 数学公式用LaTeX语法包裹在 \\( \\) 中（行内）或 \\[ \\] 中（独立行）
3. 多选题的answer包含多个字母如"AB"
4. 资料分析题需要先定义material_group，然后在question中关联（设material_group_index字段指向material_groups数组的索引）
5. 只输出JSON，不要输出其他内容"""


def get_model_by_name(model_name: str | None) -> ModelConfig:
    models = settings.get_models()
    if not model_name:
        return models[0]
    for m in models:
        if m.name == model_name:
            return m
    return models[0]


async def call_llm(
    model_name: str | None,
    module_text: str,
    answer_text: str,
    has_images: bool = False,
) -> dict:
    """Call LLM to extract questions from a module's text."""
    model = get_model_by_name(model_name)

    # Auto-downgrade if model doesn't support vision but we have images
    if has_images and not model.supports_vision:
        models = settings.get_models()
        vision_model = next((m for m in models if m.supports_vision), models[0])
        logger.info(
            "Model %s doesn't support vision, falling back to %s",
            model.name, vision_model.name,
        )
        model = vision_model

    user_content = f"""## 试卷文本（当前模块）
{module_text}

## 答案解析
{answer_text}

请将上述试卷文本中的题目提取为结构化JSON。"""

    messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": user_content},
    ]

    async with httpx.AsyncClient(timeout=300) as client:
        resp = await client.post(
            model.api_url,
            headers={
                "Authorization": f"Bearer {model.api_key}",
                "Content-Type": "application/json",
            },
            json={
                "model": model.model,
                "messages": messages,
                "max_tokens": model.max_tokens,
                "temperature": 0.1,
            },
        )
        resp.raise_for_status()
        data = resp.json()

    # Extract text from response
    content = data["choices"][0]["message"]["content"]

    # Strip markdown code fences if present
    content = content.strip()
    if content.startswith("```json"):
        content = content[7:]
    if content.startswith("```"):
        content = content[3:]
    if content.endswith("```"):
        content = content[:-3]
    content = content.strip()

    return json.loads(content)


async def get_available_models() -> list[dict]:
    """Return model list for frontend dropdown."""
    models = settings.get_models()
    return [
        {
            "name": m.name,
            "model": m.model,
            "supports_vision": m.supports_vision,
        }
        for m in models
    ]
