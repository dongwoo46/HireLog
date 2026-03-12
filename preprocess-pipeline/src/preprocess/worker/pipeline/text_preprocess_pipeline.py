import logging
from inputs.jd_preprocess_input import JdPreprocessInput

from preprocess.core_preprocess.core_preprocessor import CorePreprocessor
from preprocess.structural_preprocess.structural_preprocessor import StructuralPreprocessor
from preprocess.metadata_preprocess.metadata_preprocessor import MetadataPreprocessor
from preprocess.worker.pipeline.canonical_section_pipeline import CanonicalSectionPipeline
from preprocess.post_validation.section_post_validator import validate_section_objects

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

        logger.debug("Text core preprocessed", extra={"line_count": len(core_lines)})

        # 1.5️⃣ Metadata (문서 전역 메타)
        document_meta = self.metadata.process(core_lines)

        # 2️⃣ Structural
        # - 텍스트 레이아웃 기반 섹션 구조 생성
        sections = self.structural.process(core_lines)

        # 2.5️⃣ 섹션 구조 후보정
        sections = validate_section_objects(sections)

        logger.debug(
            "Text sections extracted",
            extra={"section_count": len(sections)},
        )

        # 3️⃣ Canonical (공통 후반 파이프라인)
        canonical_map = self.canonical.process(sections)

        logger.debug(
            "Text pipeline stages completed",
            extra={
                "section_count": len(sections),
                "canonical_zone_count": len(canonical_map),
            },
        )

        return {
            "canonical_map": canonical_map,
            "document_meta": document_meta,
        }
