from common.section.loader import load_header_keywords

def is_text_header_candidate(line: str, next_line: str | None) -> bool:
    """
    JD 섹션 헤더 판별 (Structural 단계)

    원칙:
    - 의미 해석 ❌
    - 오탐보다 미탐 허용
    - JD 레이아웃 특성에 최적화
    """

    stripped = line.strip()
    if not stripped:
        return False

    lowered = stripped.lower()

    # 0️⃣ bullet은 절대 header 아님
    if stripped.startswith("•"):
        return False

    # 1️⃣ 정책 키워드 (가장 확실)
    header_keywords = load_header_keywords()
    # 1️⃣ 완전 일치 헤더
    if lowered in header_keywords:
        return True

    # 2️⃣ 부분 일치 헤더
    for keyword in header_keywords:
        if keyword in lowered:
            return True

    # 1-2️⃣ 대괄호 / 꺾쇠 괄호
    if (
        (stripped.startswith("[") and stripped.endswith("]")) or
        (stripped.startswith("<") and stripped.endswith(">"))
    ):
        return True

    # 너무 길면 본문
    if len(stripped) > 80:
        return False

    # 괄호 포함 → 제목일 확률 낮음
    # 1️⃣ 대괄호로 감싼 단독 라인은 강한 header 시그널
    if stripped.startswith("[") and stripped.endswith("]"):
        return True
    
    # 2️⃣ 소괄호 포함 → 부연 설명일 가능성 높음
    if "(" in stripped or ")" in stripped:
        return False

    # 2️⃣ 다음 줄이 bullet이면 header
    if next_line and next_line.strip().startswith("•"):
        return True

    # 3️⃣ 콜론 종료 (영문 JD)
    if stripped.endswith(":"):
        return True

    # 4️⃣ ⭐ JD 특화 규칙 (보수적)
    # - 짧은 단독 라인
    # - 다음 줄이 문장처럼 보이지 않을 때만
    if next_line:
        next_stripped = next_line.strip()

        if (
                2 <= len(stripped) <= 15
                and not next_stripped.startswith("•")
                and len(next_stripped) >= 20          # 다음 줄이 본문처럼 길어야 함
                and (
                stripped[:1].isupper()            # 영문 헤더
                or '가' <= stripped[:1] <= '힣'   # 한글 헤더
            )
        ):
            return True

    return False
