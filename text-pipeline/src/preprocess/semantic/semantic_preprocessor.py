"""
Semantic Preprocessor (Lite)

역할:
- StructuralPreprocessor 결과에
  semantic_zone 필드를 추가한다.

아직 하지 않는 것:
- 의미 해석 ❌
- required / preferred 세부 분리 ❌
- skill 추출 ❌
"""

from dataclasses import replace

from preprocess.structural_preprocess.section_builder import Section
from .semantic_zone import detect_semantic_zone


def apply_semantic_lite(sections: list[Section]) -> list[Section]:
    """
    각 Section에 semantic_zone을 태깅한다.

    규칙:
    - semantic_zone이 이미 지정된 경우(intro 등)는 유지
    - semantic_zone == "others" 인 경우에만 header 기반 판별 수행
    """

    enriched: list[Section] = []

    for sec in sections:
        # 🔒 이미 구조 단계에서 역할이 정해진 섹션은 건드리지 않음
        if sec.semantic_zone != "others":
            enriched.append(sec)
            continue

        zone = detect_semantic_zone(sec.header)

        enriched.append(
            replace(sec, semantic_zone=zone)
        )

    return enriched
