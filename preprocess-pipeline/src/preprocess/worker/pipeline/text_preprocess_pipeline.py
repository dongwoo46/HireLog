import logging
import json
from inputs.jd_preprocess_input import JdPreprocessInput

from preprocess.core_preprocess.core_preprocessor import CorePreprocessor
from preprocess.structural_preprocess.structural_preprocessor import StructuralPreprocessor
from preprocess.metadata_preprocess.metadata_preprocessor import MetadataPreprocessor
from preprocess.worker.pipeline.canonical_section_pipeline import CanonicalSectionPipeline

logger = logging.getLogger(__name__)


class TextPreprocessPipeline:
    """
    TEXT 기반 JD 전처리 파이프라인

    책임:
    - RAW JD 텍스트 입력
    - Core → Structural 실행
    - '텍스트 기반으로' JD 구조를 생성
    - 후반 canonical 처리는 CanonicalSectionPipeline에 위임
    """

    def __init__(self):
        # 각 단계 Preprocessor는 재사용 가능하도록 멤버로 보관
        self.core = CorePreprocessor()
        self.structural = StructuralPreprocessor()
        self.metadata = MetadataPreprocessor()

        # 구조 이후 공통 후처리 파이프라인
        self.canonical = CanonicalSectionPipeline()

    def process(self, input: JdPreprocessInput) -> dict:
        """
        JD 전처리 메인 진입점 (TEXT 전용)
        """

        raw_text = input.text

        # 1️⃣ Core
        # - 줄 단위 정규화
        # - 노이즈 제거
        core_lines = self.core.process(raw_text)

        # 🔍 DEBUG: 전처리 후
        logger.debug("[TEXT_PIPELINE] 1️⃣ Core 전처리 후 (core_lines)")
        logger.debug(json.dumps(core_lines, ensure_ascii=False, indent=2))

        # 1.5️⃣ Metadata (문서 전역 메타)
        document_meta = self.metadata.process(core_lines)

        # 2️⃣ Structural
        # - 텍스트 레이아웃 기반 섹션 구조 생성
        sections = self.structural.process(core_lines)

        # 🔍 DEBUG: 섹션 분리 후
        logger.debug("[TEXT_PIPELINE] 2️⃣ 섹션 분리 후 (sections)")
        logger.debug(json.dumps(
            [{"header": s.header, "lines": s.lines, "semantic_zone": s.semantic_zone}
             for s in sections],
            ensure_ascii=False, indent=2
        ))

        # 3️⃣ Canonical (공통 후반 파이프라인)
        canonical_map = self.canonical.process(sections)

        # 🔍 DEBUG: 최종 canonical_map
        logger.debug("[TEXT_PIPELINE] 3️⃣ 최종 canonical_map")
        logger.debug(json.dumps(canonical_map, ensure_ascii=False, indent=2))

        return {
            "canonical_map": canonical_map,
            "document_meta": document_meta,
        }
