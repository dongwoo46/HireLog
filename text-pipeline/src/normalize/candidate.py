from symspellpy import SymSpell, Verbosity
from common.token.canonical import to_canonical

# ----------------------------
# SymSpell 초기화 (전역 1회)
# ----------------------------

# edit distance 2면 OCR 오타에는 충분
_symspell = SymSpell(max_dictionary_edit_distance=2, prefix_length=7)

# 기술 사전 기반으로 사전 구성
# (load_jd_vocab() 결과를 넣어도 되고, 별도 dict 파일 가능)
from common.vocab.loader import load_jd_vocab

for word in load_jd_vocab():
    _symspell.create_dictionary_entry(word, 1)


# ----------------------------
# OCR Confusable Map
# ----------------------------

_CONFUSABLE_MAP = {
    "0": ["o"],
    "1": ["l", "i"],
    "5": ["s"],
    "8": ["b"],
    "×": ["x"],
    "¥": ["y"],
}


def _confusable_variants(token: str) -> set[str]:
    """
    OCR 특수문자 치환 기반 후보 생성
    """
    variants = set()

    for i, ch in enumerate(token):
        if ch not in _CONFUSABLE_MAP:
            continue

        for repl in _CONFUSABLE_MAP[ch]:
            variants.add(token[:i] + repl + token[i + 1:])

    return variants


# ----------------------------
# 후보 생성 메인 함수
# ----------------------------

def generate_candidates(token: str, max_candidates: int = 10) -> list[str]:
    """
    깨진 토큰에 대해 교정 후보를 생성한다.

    정책:
    - 후보 생성만 수행
    - 원본 토큰 제외
    - 후보 수 제한
    """

    canonical = to_canonical(token)
    candidates: set[str] = set()

    # 1️⃣ SymSpell 후보 (편집거리 기반)
    suggestions = _symspell.lookup(
        canonical,
        Verbosity.CLOSEST,
        max_edit_distance=2
    )

    for s in suggestions:
        candidates.add(s.term)

    # 2️⃣ OCR Confusable Map 후보
    for variant in _confusable_variants(canonical):
        # variant도 SymSpell로 한 번 더 정제
        sub_suggestions = _symspell.lookup(
            variant,
            Verbosity.CLOSEST,
            max_edit_distance=1
        )
        for s in sub_suggestions:
            candidates.add(s.term)

    # 3️⃣ 원본 제거
    candidates.discard(canonical)

    # 4️⃣ 후보 수 제한 (안전장치)
    return list(candidates)[:max_candidates]
