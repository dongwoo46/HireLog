from dataclasses import dataclass
from rapidfuzz import fuzz


# ----------------------------
# 결과 객체
# ----------------------------

@dataclass
class CandidateResult:
    value: str
    score: float
    is_confident: bool


# ----------------------------
# 섹션 가중치
# ----------------------------

_SECTION_WEIGHT = {
    "required": 1.1,
    "preferred": 1.05,
    None: 1.0,
}


# ----------------------------
# 메인 선택 함수
# ----------------------------

def select_best_candidate(
        original: str,
        candidates: list[str],
        section: str | None,
) -> CandidateResult:
    """
    교정 후보 중 가장 적절한 값을 선택한다.

    정책:
    - RapidFuzz 점수 기반
    - 섹션 가중치 적용
    - 확신 없으면 교정 금지
    """

    scored = []

    for cand in candidates:
        base_score = fuzz.ratio(original.lower(), cand.lower())
        weight = _SECTION_WEIGHT.get(section, 1.0)
        final_score = base_score * weight

        scored.append((cand, final_score))

    # 점수 높은 순 정렬
    scored.sort(key=lambda x: x[1], reverse=True)

    best_value, best_score = scored[0]

    # 후보가 2개 이상이면 비교
    second_score = scored[1][1] if len(scored) > 1 else 0

    # ----------------------------
    # 확신 정책 (핵심)
    # ----------------------------

    CONFIDENCE_THRESHOLD = 88        # RapidFuzz 컷라인
    MIN_SCORE_GAP = 6                # 1위 vs 2위 차이

    is_confident = (
            best_score >= CONFIDENCE_THRESHOLD
            and (best_score - second_score) >= MIN_SCORE_GAP
    )

    return CandidateResult(
        value=best_value,
        score=best_score,
        is_confident=is_confident
    )
