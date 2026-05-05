import re
import logging
from dataclasses import dataclass, field

logger = logging.getLogger(__name__)


@dataclass
class ModuleBlock:
    name: str            # e.g. "政治理论"
    start_line: int      # inclusive
    end_line: int        # exclusive
    questions: list[dict] = field(default_factory=list)


@dataclass
class QuestionSpan:
    number: int
    start_line: int
    end_line: int


# Known module names in 行测 exams
MODULE_NAMES = [
    "政治理论",
    "常识判断",
    "言语理解与表达",
    "数量关系",
    "判断推理",
    "资料分析",
]

# CJK compatibility radicals → standard CJK chars (extended for numerals)
_CJK_RADICAL_MAP = str.maketrans({
    "\u2f00": "一", "\u2f01": "丨", "\u2f02": "丶", "\u2f03": "丿",
    "\u2f04": "乙", "\u2f05": "亅", "\u2f06": "二", "\u2f07": "亠",
    "\u2f08": "人", "\u2f09": "儿", "\u2f0a": "入", "\u2f0b": "八",
    "\u2f0c": "冂", "\u2f0d": "刀", "\u2f0e": "力", "\u2f0f": "勹",
    "\u2f10": "匕", "\u2f11": "匚", "\u2f12": "匸", "\u2f13": "十",
    "\u2f14": "卜", "\u2f15": "卩", "\u2f16": "厂", "\u2f17": "厶",
    "\u2f18": "又", "\u2f19": "口", "\u2f1a": "囗",
    # Compatibility ideographs for Chinese numerals
    "\uf961": "三", "\uf962": "四", "\uf963": "五",
    # Kangxi radicals that might appear as numerals
    "\u2f1b": "三",
})

# Build alternation for module names
_module_names_re = "|".join(MODULE_NAMES)

# Patterns for module headers, supporting multiple formats:
# 一、政治理论 / 一.政治理论 / 第一部分 政治理论 / （一）政治理论 / Part一 政治理论
MODULE_HEADER_RE = re.compile(
    r"(?:"
    r"第[一二三四五六七八九十]+[部分]"          # 第三部分
    r"|"
    r"[（(][一二三四五六七八九十]+[）)]"          # （三）
    r"|"
    r"[一二三四五六七八九十]+[.、．:：]"          # 三、
    r")"
    r"\s*"
    r"(" + _module_names_re + ")"
)


def detect_modules(text: str) -> list[ModuleBlock]:
    """Split text into module blocks based on section headers."""
    normalized = text.translate(_CJK_RADICAL_MAP)
    lines = normalized.split("\n")
    modules: list[ModuleBlock] = []
    current_module: ModuleBlock | None = None

    for i, line in enumerate(lines):
        m = MODULE_HEADER_RE.search(line)
        if m:
            if current_module is not None:
                current_module.end_line = i
            current_module = ModuleBlock(name=m.group(1), start_line=i, end_line=len(lines))
            modules.append(current_module)
            logger.info("Detected module '%s' at line %d: %s", m.group(1), i, line.strip()[:80])

    # If no modules detected, create a single default module
    if not modules:
        logger.warning("No modules detected, using default '未分类'")
        modules = [ModuleBlock(name="未分类", start_line=0, end_line=len(lines))]

    # Log missing modules
    detected_names = {mod.name for mod in modules}
    for name in MODULE_NAMES:
        if name not in detected_names:
            # Search for lines that might contain the module name
            for i, line in enumerate(lines):
                if name[:4] in line and not MODULE_HEADER_RE.search(line):
                    logger.warning("Module '%s' not matched but name found at line %d: %s", name, i, line.strip()[:80])

    # Set end lines
    for i in range(len(modules) - 1):
        modules[i].end_line = modules[i + 1].start_line

    return modules


def split_text_by_modules(text: str) -> dict[str, str]:
    """Return a dict of module_name -> text_content for that module."""
    modules = detect_modules(text)
    # Use original text lines for content, but module boundaries from normalized text
    orig_lines = text.split("\n")
    result: dict[str, str] = {}
    for mod in modules:
        mod_lines = orig_lines[mod.start_line:mod.end_line]
        result[mod.name] = "\n".join(mod_lines)
    return result
