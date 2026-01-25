import re

# =========================
# JD 헤더 / 메타 보호 키워드
# =========================
JD_HEADER_KEYWORDS = {
    "전형절차", "채용절차", "전형안내", "안내사항",
    "우대사항", "자격요건", "지원자격",
    "근무조건", "근무형태", "고용형태",
    "모집요강", "채용정보", "지원방법",
    "인터뷰", "코딩테스트", "culture", "culture-fit",
    "정규직", "계약직", "인턴"
}

# =========================
# 조사 / 어미 패턴
# =========================
KOREAN_PARTICLE_PATTERN = re.compile(
    r"(은|는|이|가|을|를|의|에|에서|로|으로|와|과|도|만|"
    r"하다|되다|있다|없다|이다|하고|이며|입니다|합니다|"
    r"된|할|한|등|및|적)"
)

# =========================
# 명백한 OCR 깨짐 패턴
# =========================
GARBLED_PATTERNS = [
    re.compile(r"[가-힣][A-Z][가-힣]"),        # 위Y거로우로
    re.compile(r"[가-힣]{1,2}[*\(\)]+[가-힣]"), # 글(*자
    re.compile(r"[ㄱ-ㅎㅏ-ㅣ]{2,}"),           # ㅇㅆ, ㄱㅏ
]


def is_garbled_korean(text: str) -> bool:
    """
    OCR로 깨진 한국어 노이즈 판별

    True  → 제거 대상
    False → 유지 대상
    """

    if not text:
        return True

    text = text.strip()
    if len(text) < 2:
        return True

    compact = text.replace(" ", "").lower()

    # =========================
    # 0️⃣ JD 헤더 / 메타는 무조건 보호
    # =========================
    for kw in JD_HEADER_KEYWORDS:
        if kw.replace(" ", "").lower() in compact:
            return False

    # =========================
    # 1️⃣ 명백한 깨짐 패턴
    # =========================
    for pattern in GARBLED_PATTERNS:
        if pattern.search(text):
            return True

    # =========================
    # 2️⃣ 한글 비율 계산
    # =========================
    korean_chars = sum(1 for c in text if '가' <= c <= '힣')
    total_chars = len(text)

    if korean_chars == 0:
        # 영문 전용은 여기서 판단하지 않음
        return False

    korean_ratio = korean_chars / total_chars

    # =========================
    # 3️⃣ 짧은 텍스트 (≤ 12자)
    # =========================
    if len(text) <= 12:
        # 한글 위주인데 조사/어미 없음 → 깨진 확률 높음
        if korean_ratio > 0.9 and not KOREAN_PARTICLE_PATTERN.search(text):
            return True

        # 한글 + 대문자 1글자 섞임
        upper_count = sum(1 for c in text if c.isupper())
        if korean_ratio >= 0.7 and upper_count == 1:
            return True

    # =========================
    # 4️⃣ 중간 길이 (13 ~ 30자)
    # =========================
    elif 13 <= len(text) <= 30:
        if korean_ratio > 0.8 and not KOREAN_PARTICLE_PATTERN.search(text):
            return True

        special_chars = sum(
            1 for c in text
            if not (c.isalnum() or c.isspace() or c in ":-,.()/▶")
        )
        if special_chars / total_chars > 0.3:
            return True

    # =========================
    # 5️⃣ 긴 텍스트 (31자 이상)
    # =========================
    else:
        if korean_ratio > 0.7 and not KOREAN_PARTICLE_PATTERN.search(text):
            return True

    return False
