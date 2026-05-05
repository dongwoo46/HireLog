import re
from common.section.loader import load_header_keywords


def _normalize_header_compact(text: str) -> str:
    lowered = (text or "").strip().lower()
    return re.sub(r"[\s\[\]\(\)<>【】:;,.·•\-_/]+", "", lowered)

_HARD_HEADER_MARKERS = (
    "주요업무",
    "담당업무",
    "업무내용",
    "자격요건",
    "필수요건",
    "지원자격",
    "우대사항",
    "우대조건",
    "기술스택",
    "기술요건",
    "requirements",
    "qualifications",
    "preferred",
    "nicetohave",
    "techstack",
)

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

    # 공백 제거 버전으로도 비교 (토스 스타일 대응)
    lowered_no_space = lowered.replace(" ", "")
    lowered_compact = _normalize_header_compact(stripped)

    # 0.5️⃣ 핵심 헤더 하드 매칭
    if any(marker == lowered_compact for marker in _HARD_HEADER_MARKERS):
        return True

    # 1️⃣ 완전 일치 헤더
    if lowered in header_keywords:
        return True

    # 2️⃣ 부분 일치 헤더 (대괄호 안 내용도 매칭)
    # "[주요업무]" → "주요업무" in "[주요업무]" → True
    # 토스 스타일: "합류하면 함께할 업무예요" → "함께할업무" 포함
    is_bracket_phrase = stripped.startswith("[") and stripped.endswith("]")
    inner_compact = _normalize_header_compact(stripped[1:-1]) if is_bracket_phrase else lowered_compact

    for keyword in header_keywords:
        kw_no_space = keyword.replace(" ", "")
        kw_compact = _normalize_header_compact(keyword)
        if not kw_compact:
            continue

        # 완전 일치(구두점/공백 무시)는 즉시 허용
        if lowered_compact == kw_compact:
            return True

        matched = (
            keyword in lowered
            or kw_no_space in lowered_no_space
            or kw_compact in lowered_compact
            or lowered_compact in kw_compact
            or lowered_compact.startswith(kw_compact)
        )
        if matched:
            coverage = len(kw_compact) / max(len(lowered_compact), 1)
            # 짧은 일반 단어(예: "경험")의 본문 오탐 방지
            if len(kw_compact) < 4 and coverage < 0.8:
                continue
            if len(kw_compact) >= 4 and coverage < 0.45 and not lowered_compact.startswith(kw_compact):
                continue

            # [대괄호 구문] + 짧은 키워드 부분일치 → 오탐 방지
            # 예: [Mission of the Role] + "role"(4자) → 제외
            # 예: [Key Responsibilities] + "responsibilities"(16자) → 허용
            # 예: [주요업무] + "주요업무"(4자) → 완전 일치이므로 허용
            if is_bracket_phrase and len(kw_no_space) < 6:
                if kw_compact != inner_compact:
                    continue
            return True

    # 3️⃣ 대괄호 / 꺾쇠 괄호 → 대분류(container)로 취급, header 아님
    # 단, 위에서 header_keywords 매칭되면 이미 True 반환됨
    # 예: [포지션 상세] → header_keywords에 없음 → 대분류 (header 아님)
    # 예: [주요업무] → header_keywords에 "주요업무" 있음 → 위에서 True 반환
    if (
        (stripped.startswith("[") and stripped.endswith("]")) or
        (stripped.startswith("<") and stripped.endswith(">"))
    ):
        return False  # 대분류는 header 아님

    # 너무 길면 본문
    if len(stripped) > 80:
        return False

    # 소괄호 포함 → 부연 설명일 가능성 높음
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
        token_count = len(stripped.split())

        if (
                2 <= len(stripped) <= 15
                and token_count <= 2
                and not next_stripped.startswith("•")
                and len(next_stripped) >= 20          # 다음 줄이 본문처럼 길어야 함
                and (
                stripped[:1].isupper()            # 영문 헤더
                or '가' <= stripped[:1] <= '힣'   # 한글 헤더
            )
        ):
            return True

    return False
