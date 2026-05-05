import re
from dataclasses import dataclass, field


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

# Patterns for module headers: "一. 政治理论" or "一、政治理论" etc.
MODULE_HEADER_RE = re.compile(
    r"[一二三四五六七八九十]+[.、．]\s*("
    + "|".join(MODULE_NAMES)
    + ")"
)


def detect_modules(text: str) -> list[ModuleBlock]:
    """Split text into module blocks based on section headers."""
    lines = text.split("\n")
    modules: list[ModuleBlock] = []
    current_module: ModuleBlock | None = None

    for i, line in enumerate(lines):
        m = MODULE_HEADER_RE.search(line)
        if m:
            if current_module is not None:
                current_module.end_line = i
            current_module = ModuleBlock(name=m.group(1), start_line=i, end_line=len(lines))
            modules.append(current_module)

    # If no modules detected, create a single default module
    if not modules:
        modules = [ModuleBlock(name="未分类", start_line=0, end_line=len(lines))]

    # Set end lines
    for i in range(len(modules) - 1):
        modules[i].end_line = modules[i + 1].start_line

    return modules


def split_text_by_modules(text: str) -> dict[str, str]:
    """Return a dict of module_name -> text_content for that module."""
    modules = detect_modules(text)
    lines = text.split("\n")
    result: dict[str, str] = {}
    for mod in modules:
        mod_lines = lines[mod.start_line:mod.end_line]
        result[mod.name] = "\n".join(mod_lines)
    return result
