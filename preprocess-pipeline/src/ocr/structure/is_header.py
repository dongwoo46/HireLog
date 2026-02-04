from common.section.loader import load_header_keywords

HEADER_MAX_LENGTH = 40
HEADER_MAX_LENGTH_EXTENDED = 60  # 토스 스타일 문장형 헤더용
HEADER_MAX_TOKENS = 6
HEADER_MAX_TOKENS_EXTENDED = 10  # 토스 스타일 문장형 헤더용


def is_ocr_header_line(line: dict) -> bool:
    """
    JD 헤더(header) 여부를 보수적으로 판정한다.

    판정 원칙:
    - 키워드 포함은 필수
    - 짧고 명확한 문구
    - 설명 문장 배제
    - OCR confidence 최소 기준
    - 시각적 신호(header_score / height)로 보강

    토스 스타일 대응:
    - 문장형 헤더("~예요", "~드려요")도 키워드 매칭 시 허용
    - 키워드 길이가 충분하면 토큰/길이 제한 완화
    """

    text = line.get("text", "").strip()
    if not text:
        return False

    text_lower = text.lower()
    text_no_space = text_lower.replace(" ", "")

    # 0️⃣ bullet/숫자 시작은 즉시 제외
    if text.startswith(("·", "-", "•", "*")):
        return False
    if text[0].isdigit():
        return False

    # 1️⃣ 키워드 매칭 먼저 체크 (토스 스타일 대응)
    header_keywords = load_header_keywords()
    matched_keyword = _get_matched_keyword(text_lower, text_no_space, header_keywords)

    if matched_keyword:
        # 키워드 길이가 충분하면 (토스 스타일) 제한 완화
        is_long_keyword = len(matched_keyword.replace(" ", "")) >= 6

        # 토스 스타일: 확장된 길이/토큰 제한 적용
        max_length = HEADER_MAX_LENGTH_EXTENDED if is_long_keyword else HEADER_MAX_LENGTH
        max_tokens = HEADER_MAX_TOKENS_EXTENDED if is_long_keyword else HEADER_MAX_TOKENS

        if len(text) > max_length:
            return False
        if line.get("token_count", 0) > max_tokens:
            return False

        # 토스 스타일(긴 키워드)이면 문장형 검사 스킵
        if not is_long_keyword and _looks_like_sentence(text_lower):
            return False

        # 시각적 보강 또는 키워드 매칭만으로 header 인정
        return True

    # 2️⃣ 키워드 미매칭 → 기존 보수적 로직
    if len(text) > HEADER_MAX_LENGTH:
        return False
    if line.get("token_count", 0) > HEADER_MAX_TOKENS:
        return False
    if _looks_like_sentence(text_lower):
        return False

    # 시각적 신호만으로 header 판정 (키워드 없는 경우)
    if _visual_header_signal(line):
        return True

    return False


def _get_matched_keyword(text_lower: str, text_no_space: str, keywords: set) -> str | None:
    """
    텍스트에서 매칭되는 키워드 반환

    매칭 방식:
    - 공백 제거 후 포함 여부 체크
    - 가장 긴 매칭 키워드 반환 (정확도 향상)
    """
    matched = None
    matched_len = 0

    for kw in keywords:
        kw_no_space = kw.replace(" ", "")
        if kw in text_lower or kw_no_space in text_no_space:
            if len(kw_no_space) > matched_len:
                matched = kw
                matched_len = len(kw_no_space)

    return matched

def _looks_like_sentence(text: str) -> bool:
    """
    설명 문장처럼 보이는 텍스트 제거
    """

    # 명확한 문장 종결
    if text.endswith(".") or text.endswith("다") or text.endswith("다."):
        return True

    # 설명형 조사/어미
    sentence_markers = [
        "합니다", "됩니다", "있습니다",
        "하는 ", "하며", "및 ",
        "으로 ", "에서 ", "하여 ",
    ]

    return any(marker in text for marker in sentence_markers)

def _visual_header_signal(line: dict) -> bool:
    """
    OCR 결과 기반 시각적 헤더 신호
    """

    # header_score는 최강 신호
    if line.get("header_score", 0) >= 4:
        return True

    # 상대적으로 큰 글자
    if line.get("height", 0) >= 45:
        return True

    # 영문 JD: 콜론 종료
    text = line.get("text", "")
    if text.endswith(":"):
        return True

    return False

def normalize_header_key(text: str) -> str:
    """
    OCR 헤더 텍스트를 '키로 쓰기 적합한 형태'로만 정규화한다.
    의미 변환은 하지 않는다.
    """

    return (
        text.strip()
        .lower()
        .replace(" ", "")
    )
