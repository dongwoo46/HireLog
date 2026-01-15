import re
from wordfreq import zipf_frequency


# OCR에서 자주 깨지는 특수문자
_OCR_CONFUSABLE_CHARS = r"[×¥¬¦§]"

# 알파벳 + 숫자/기호 혼합 패턴
_ALPHA_MIXED = re.compile(r"[A-Za-z].*[0-9×¥]|[0-9×¥].*[A-Za-z]")

# 반복 문자 (garbage는 아니지만 broken 후보)
_REPEATED = re.compile(r"(.)\1{2,}")

def looks_broken(token: str) -> bool:
    """
    토큰이 '명백히 OCR 깨짐'인지 판단한다.

    의미:
    - 교정해서 정상 단어가 될 가능성이 높은 경우만 True
    - 애매하면 False
    """

    if not token:
        return False

    t = token.strip()

    # 0️⃣ 너무 짧은 토큰은 교정 대상 아님
    if len(t) <= 2:
        return False

    # 알파벳 개수
    alpha_cnt = sum(c.isalpha() for c in t)

    # 알파벳이 거의 없으면 교정 불가
    if alpha_cnt < 2:
        return False

    # 1️⃣ 알파벳 + 숫자 혼합 (jav4, spr1ng)
    if re.search(r"[A-Za-z]", t) and re.search(r"[0-9]", t):
        return True

    # 2️⃣ 알파벳 + OCR confusable 문자
    if re.search(_OCR_CONFUSABLE_CHARS, t) and re.search(r"[A-Za-z]", t):
        return True

    # 3️⃣ 알파벳 위주인데 특수문자가 약간 섞인 경우
    # 예: qrpc, k8s
    non_alpha_ratio = (len(t) - alpha_cnt) / len(t)
    if non_alpha_ratio <= 0.3 and alpha_cnt >= 4:
        return True

    return False

