from ocr.structure.is_header import is_ocr_header_line, normalize_header_key

INTRO_KEY = "__intro__"

def extract_sections_by_header(lines: list[dict]) -> dict[str, list[str]]:
    """
    OCR 라인 목록에서 header를 기준으로
    각 header에 속하는 '텍스트' 라인들을 추출한다.

    기준:
    - header를 만나면 새 섹션 시작
    - 다음 header 전까지 모든 라인은 해당 섹션에 귀속
    - header 이전 라인은 intro로 수집
    """

    sections: dict[str, list[str]] = {
        INTRO_KEY: []
    }

    current_header: str | None = None

    for line in lines:
        text = line.get("text", "").strip()
        if not text:
            continue

        # 1️⃣ 헤더 판정
        if is_ocr_header_line(line):
            header_key = normalize_header_key(text)
            current_header = header_key
            sections.setdefault(current_header, [])
            continue

        # 2️⃣ header 이전 → intro
        if current_header is None:
            sections[INTRO_KEY].append(text)
            continue

        # 3️⃣ body → 현재 header에 귀속
        sections[current_header].append(text)

    # intro가 비어 있으면 제거
    if not sections[INTRO_KEY]:
        sections.pop(INTRO_KEY)

    return sections
