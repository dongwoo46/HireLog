from typing import List
from preprocess.structural_preprocess.section_builder import Section

# JD 의미상 제거해도 되는 섹션 키워드
DROP_SECTION_KEYWORDS = (
    "유의사항",
    "마감일",
    "참고사항",
    "안내사항",
    "기타사항",
    "notice",
    "disclaimer",
)


def filter_irrelevant_sections(sections: List[Section]) -> List[Section]:
    """
    JD 의미와 직접 관련 없는 섹션 제거

    제거 대상:
    - 유의사항
    - 마감일
    - 법적/안내성 섹션

    주의:
    - header 기준으로만 판단한다
    - semantic_zone == others라도 무조건 제거하지는 않는다
    """

    filtered: List[Section] = []

    for sec in sections:
        header = sec.header or ""

        if any(k in header for k in DROP_SECTION_KEYWORDS):
            continue

        filtered.append(sec)

    return filtered
