import re
from wordfreq import zipf_frequency


# 특수문자만 있거나 의미 없는 기호 패턴
_ONLY_SYMBOLS = re.compile(r'^[^a-zA-Z0-9가-힣]+$')

# 숫자/기호가 과도하게 섞인 토큰
_TOO_MANY_SYMBOLS = re.compile(r'[^\w가-힣]')

# 반복 문자 (OCR 깨짐 대표 패턴)
_REPEATED_CHARS = re.compile(r'(.)\1{3,}')


def looks_garbage(token: str) -> bool:
    """
    토큰이 '확실한 쓰레기'인지 판단한다.

    정책:
    - 애매하면 False
    - 한국어 포함 토큰은 최대한 보호
    - 제거는 '명백한 쓰레기'만
    """

    if not token:
        return True

    t = token.strip()

    # 1️⃣ 길이 기준
    if len(t) <= 1:
        return True

    # 2️⃣ 특수문자만 있는 경우
    if _ONLY_SYMBOLS.match(t):
        return True

    # 3️⃣ 반복 문자 패턴 (OCR 깨짐)
    if _REPEATED_CHARS.search(t):
        return True

    # ─────────────────────────────
    # ⭐ 핵심 분기: 한글 포함 여부
    # ─────────────────────────────
    has_korean = any('가' <= c <= '힣' for c in t)

    if has_korean:
        # 한글이 포함된 경우
        # → garbage 판단을 매우 보수적으로
        return False

    # ─────────────────────────────
    # 이하 로직은 "영문/기술 토큰" 전용
    # ─────────────────────────────

    # 4️⃣ 알파벳 비율 검사 (영문)
    alpha_cnt = sum(c.isalpha() for c in t)
    ratio = alpha_cnt / len(t)

    if ratio < 0.3:
        return True

    # 5️⃣ 특수문자 비율 과다
    symbol_cnt = len(_TOO_MANY_SYMBOLS.findall(t))
    if symbol_cnt / len(t) > 0.5:
        return True

    # 6️⃣ 자연어 빈도 검사 (영문만)
    freq = zipf_frequency(t.lower(), "en")

    # 완전히 쓰이지 않는 영문 문자열
    if freq == 0 and len(t) >= 5:
        return True

    return False

