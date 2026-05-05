"""Parse 3 HTML files (questions, answers, explanations) into structured paper data."""

import re
from bs4 import BeautifulSoup, Tag
from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel


class OptionItem(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    label: str
    text: str
    image: str = ""


class ParsedQuestion(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    sort_order: int
    content: str
    options: list[OptionItem]
    answer: str = ""
    explanation: str = ""
    images: list[str] = []
    type: str = "single_choice"
    module: str = ""
    material_group_index: int | None = None


class ParsedMaterialGroup(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    title: str
    content: str
    images: list[str] = []
    sort_order: int = 0
    question_numbers: list[int] = []


class ParsedSection(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    module: str
    description: str
    questions: list[ParsedQuestion]


class ParsedMetadata(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    title: str = ""
    year: int | None = None
    category: str = "行测"
    region_name: str = ""


class ParsedPaper(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    metadata: ParsedMetadata
    sections: list[ParsedSection]
    material_groups: list[ParsedMaterialGroup]


MODULE_MAP = {
    "政治理论": "政治理论",
    "常识判断": "常识判断",
    "言语理解": "言语理解",
    "数量关系": "数量关系",
    "判断推理": "判断推理",
    "资料分析": "资料分析",
}


def parse_module(section_title: str) -> str:
    for key, value in MODULE_MAP.items():
        if key in section_title:
            return value
    return section_title.split("、", 1)[1].split("。")[0] if "、" in section_title else section_title


def extract_metadata(soup: BeautifulSoup) -> ParsedMetadata:
    title_tag = soup.find("h3", align="center")
    title = title_tag.get_text(strip=True) if title_tag else ""

    year_match = re.search(r"(\d{4})年", title)
    year = int(year_match.group(1)) if year_match else None

    category = "行测" if "行测" in title else ("申论" if "申论" in title else "行测")

    if "国家" in title or "国考" in title:
        region_name = "国考"
    else:
        region_name = ""

    return ParsedMetadata(title=title, year=year, category=category, region_name=region_name)


def parse_answer_map(html_content: str) -> dict[int, str]:
    matches = re.findall(r"(\d+)、([A-D])", html_content)
    return {int(num): letter for num, letter in matches}


def parse_option_div(div: Tag) -> OptionItem:
    text = div.get_text(strip=True)
    match = re.match(r"^([A-F])[、.:：]\s*(.*)", text, re.DOTALL)
    if match:
        label = match.group(1)
        option_text = match.group(2).strip()
    else:
        label = ""
        option_text = text

    img = ""
    img_tag = div.find("img")
    if img_tag and img_tag.get("src"):
        img = img_tag["src"]

    return OptionItem(label=label, text=option_text, image=img)


def extract_images(element: Tag) -> list[str]:
    imgs = []
    for img_tag in element.find_all("img"):
        src = img_tag.get("src", "")
        if src:
            imgs.append(src)
    return imgs


def parse_question_row(row: Tag) -> tuple[int, str, list[OptionItem], list[str]]:
    left = row.find("div", class_="left")
    right = row.find("div", class_="right")

    number = int(left.get_text(strip=True)) if left else 0

    options = []
    images = []
    content = ""

    if right:
        images = extract_images(right)

        for child in right.children:
            if not isinstance(child, Tag):
                continue
            classes = child.get("class", [])
            col_classes = {"col-xs-3", "col-xs-6", "col-xs-12"}
            if set(classes) & col_classes:
                opt = parse_option_div(child)
                if opt.label:
                    options.append(opt)

        content_tags = []
        for child in right.children:
            if not isinstance(child, Tag):
                continue
            if child.name == "p":
                # LaTeX formula images (flag="tex") should be inline
                for img in child.find_all("img", attrs={"flag": "tex"}):
                    style = img.get("style", "")
                    style = style.replace("display: block", "display: inline")
                    img["style"] = style
                content_tags.append(str(child))

        content = "\n".join(content_tags)

    return number, content, options, images


def parse_questions_file(html_content: str) -> tuple[ParsedMetadata, list[ParsedSection], list[ParsedMaterialGroup]]:
    soup = BeautifulSoup(html_content, "lxml")
    metadata = extract_metadata(soup)

    body = soup.find("body")
    if not body:
        return metadata, [], []

    rows = body.find_all("div", class_="row")

    sections = []
    material_groups = []
    current_section = None
    current_material_group = None
    material_group_sort = 0

    for row in rows:
        subtitle = row.find("div", class_="subtitle")
        if subtitle:
            section_text = subtitle.get_text(strip=True)
            module = parse_module(section_text)
            description = section_text.split("。", 1)[1].strip() if "。" in section_text else ""
            current_section = ParsedSection(module=module, description=description, questions=[])
            sections.append(current_section)
            current_material_group = None
            continue

        sub2title = row.find("div", class_="sub2title")
        if sub2title:
            title = sub2title.get_text(strip=True)
            material_group_sort += 1
            # Find the content div: col-xs-12 but NOT the sub2title itself
            content_div = None
            for candidate in row.find_all("div", class_="col-xs-12"):
                if "sub2title" not in candidate.get("class", []):
                    content_div = candidate
                    break
            if content_div:
                content_html = "".join(str(c) for c in content_div.children)
                content_images = extract_images(content_div)
            else:
                content_html = ""
                content_images = []

            current_material_group = ParsedMaterialGroup(
                title=title,
                content=content_html.strip(),
                images=content_images,
                sort_order=material_group_sort,
                question_numbers=[]
            )
            material_groups.append(current_material_group)
            continue

        left = row.find("div", class_="left")
        if left and current_section is not None:
            number, content, options, images = parse_question_row(row)
            q = ParsedQuestion(
                sort_order=number,
                content=content,
                options=options,
                images=images,
                module=current_section.module,
            )
            if current_material_group is not None:
                q.material_group_index = len(material_groups) - 1
                current_material_group.question_numbers.append(number)

            current_section.questions.append(q)

    return metadata, sections, material_groups


def parse_explanations_file(html_content: str) -> dict[int, str]:
    soup = BeautifulSoup(html_content, "lxml")
    body = soup.find("body")
    if not body:
        return {}

    rows = body.find_all("div", class_="row")
    explanations = {}

    for row in rows:
        left = row.find("div", class_="left")
        right = row.find("div", class_="right")
        if left and right:
            number = int(left.get_text(strip=True))
            exp_parts = [str(p) for p in right.find_all("p")]
            explanations[number] = "\n".join(exp_parts)

    return explanations


def parse_all(questions_html: str, answers_html: str, explanations_html: str) -> ParsedPaper:
    metadata, sections, material_groups = parse_questions_file(questions_html)
    answer_map = parse_answer_map(answers_html)
    explanation_map = parse_explanations_file(explanations_html)

    for section in sections:
        for q in section.questions:
            q.answer = answer_map.get(q.sort_order, "")
            q.explanation = explanation_map.get(q.sort_order, "")

    return ParsedPaper(
        metadata=metadata,
        sections=sections,
        material_groups=material_groups,
    )
