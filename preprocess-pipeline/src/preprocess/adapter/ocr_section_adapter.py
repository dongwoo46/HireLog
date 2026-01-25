# preprocess/adapter/ocr_section_adapter.py

from preprocess.structural_preprocess.section_builder import Section
from ocr.structure.header_grouping import INTRO_KEY


def adapt_ocr_sections_to_sections(
    raw_sections: dict[str, list[str]]
) -> list[Section]:
    """
    OCR header grouping 결과를
    Canonical 파이프라인이 소비 가능한 Section 구조로 변환한다.

    핵심 설계:
    - OCR 단계에서 구조는 이미 확정되었다
    - Section 분리는 최소화한다
    - header는 semantic_zone 후보로만 사용한다
    """

    sections: list[Section] = []

    for header_key, lines in raw_sections.items():
        if not lines:
            continue

        # intro → 의미 없는 설명 영역
        if header_key == INTRO_KEY:
            sections.append(
                Section(
                    header=None,
                    lines=lines,
                    lists=[],
                    semantic_zone="intro"
                )
            )
            continue

        # 일반 header → semantic 후보
        sections.append(
            Section(
                header=header_key,     # semantic-lite에서 해석
                lines=lines,
                lists=[],
                semantic_zone="others" # semantic 단계에서 재지정
            )
        )

    return sections
