from preprocess.structural_preprocess.section_builder import Section

# JD 의미상 제거해도 되는 섹션 헤더 (정규화 기준)
DROP_SECTION_HEADERS = {
    "유의사항",
    "마감일",
    "참고사항",
    "안내사항",
    "기타사항",
    "notice",
    "disclaimer",
}


def normalize_header_for_drop(header: str) -> str:
    """
    DROP 판정을 위한 header 정규화

    정책:
    - 공백 제거
    - 소문자 변환
    - 의미 해석 ❌
    """
    return header.replace(" ", "").lower()


def filter_irrelevant_sections(sections: list[Section]) -> list[Section]:
    """
    JD 의미와 직접 관련 없는 섹션 제거

    제거 대상:
    - 섹션 전체가 안내/법적 성격인 경우만 제거

    주의:
    - header 기준으로만 판단한다
    - 복합 헤더(예: '전형절차 및 기타사항')는 제거하지 않는다
    """

    filtered: list[Section] = []

    for sec in sections:
        if not sec.header:
            filtered.append(sec)
            continue

        normalized = normalize_header_for_drop(sec.header)

        # 🔒 완전 일치인 경우만 DROP
        if normalized in DROP_SECTION_HEADERS:
            continue

        filtered.append(sec)

    return filtered
