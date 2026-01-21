def normalize_line(line):
    """
    JD 라인을 라인 단위로 최소 정규화한다.

    책임:
    - 구조적으로 의미 없는 라인만 제거
    - JD 의미 판단이나 기술 키워드 제거는 하지 않음
    """

    text = line["text"].strip()

    # 1️⃣ 완전히 빈 라인 제거
    if not text:
        return None

    # 2️⃣ 단일 문자 라인 제거 (예: "-", "•")
    if len(text) == 1:
        return None

    # 3️⃣ 단어 1개라도 길이가 충분하면 유지
    # 예: "Java", "AWS", "Spring"
    tokens = text.split()
    if len(tokens) == 1 and len(tokens[0]) <= 2:
        return None

    return line
