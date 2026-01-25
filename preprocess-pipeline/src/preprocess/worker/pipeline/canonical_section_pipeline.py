from collections import defaultdict
from preprocess.semantic.semantic_preprocessor import apply_semantic_lite
from preprocess.semantic.section_filter import filter_irrelevant_sections


class CanonicalSectionPipeline:
    """
    구조화된 Section 목록을 입력으로 받아
    semantic-lite → filter → canonical_map 을 생성한다.

    책임:
    - 이미 '의미 단위로 구조화된 sections'만 처리
    - Core / Structural 단계에는 관여하지 않는다
    - TEXT / OCR / URL 파이프라인에서 공통으로 재사용된다
    """

    def process(self, sections) -> dict[str, list[str]]:
        # 1️⃣ Semantic-lite (의미 구역 보정)
        sections = apply_semantic_lite(sections)

        # 2️⃣ 불필요 섹션 제거
        sections = filter_irrelevant_sections(sections)

        # 3️⃣ Canonical Map 생성
        return self._build_canonical_map(sections)

    def _build_canonical_map(self, sections) -> dict[str, list[str]]:
        """
        semantic_zone 기준 key:value canonical 구조 생성
        """

        result = defaultdict(list)

        for sec in sections:
            zone = sec.semantic_zone

            # 일반 문장
            for line in sec.lines:
                result[zone].append(line)

            # 리스트 항목
            for lst in sec.lists:
                for item in lst:
                    result[zone].append(item)

        return dict(result)
