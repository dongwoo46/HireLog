from jd.vocab import is_known_vocab
from jd.garbage import looks_garbage
from jd.broken import looks_broken
from jd.candidate import generate_candidates
from jd.select import select_best_candidate


def normalize_token(
        token: str,
        section: str | None,
        vocab: set,
) -> str | None:
    """
    JD 도메인 기준 토큰 정규화

    정책:
    - 애매하면 절대 고치지 않음
    """

    # 1️⃣ garbage 제거
    if looks_garbage(token):
        return None

    # 2️⃣ 정상 보호
    if is_known_vocab(token, vocab):
        return token

    # 3️⃣ 정상 통과 - 깨지지 않았으면 그대로
    if not looks_broken(token):
        return token

    # 4️⃣ 후보 생성 (SymSpell + OCR confusable) 토큰 교정
    candidates = generate_candidates(token)

    if not candidates:
        return token

    # 5️⃣ 후보 평가 (RapidFuzz + 도메인 가중치)
    best = select_best_candidate(
        original=token,
        candidates=candidates,
        section=section
    )

    # 6️⃣ 확신 정책
    if not best.is_confident:
        return token

    return best.value
