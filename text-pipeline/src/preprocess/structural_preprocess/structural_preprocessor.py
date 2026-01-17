"""
Structural Preprocessor

역할:
- Core Preprocessing 결과를
  '구조화된 JD 문서'로 변환한다.

포함 기능:
- Header Grouping
- Section 분리
- Bullet List 묶기

아직 하지 않는 것:
- 의미 해석 ❌
- Required / Preferred 판단 ❌
- Skill 추출 ❌
"""

from typing import List

from .section_builder import build_sections, Section
from .list_grouper import group_lists


class StructuralPreprocessor:
    """
    JD Structural Preprocessing 파이프라인
    """

    def process(self, lines: List[str]) -> List[Section]:
        """
        Structural Preprocessing 단일 진입점

        입력:
            lines: Core Preprocessing 완료된 라인 문서

        출력:
            List[Section]
        """

        # 1️⃣ header 기준으로 섹션 분리
        sections = build_sections(lines)

        # 2️⃣ 각 섹션 내부에서 bullet 리스트 묶기
        return [group_lists(sec) for sec in sections]
