import logging
import json
from inputs.jd_preprocess_input import JdPreprocessInput
from ocr.pipeline import process_ocr_input
from ocr.structure.header_grouping import extract_sections_by_header
from preprocess.worker.pipeline.canonical_section_pipeline import CanonicalSectionPipeline
from preprocess.metadata_preprocess.metadata_preprocessor import MetadataPreprocessor
from preprocess.adapter.ocr_section_adapter import adapt_ocr_sections_to_sections
from preprocess.post_validation.section_post_validator import validate_raw_sections

logger = logging.getLogger(__name__)

_SEPARATOR = "=" * 60


def _debug_ocr_raw(ocr_result: dict) -> None:
    """OCR 원본 추출 데이터"""
    lines = ocr_result.get("lines", [])

    logger.debug("\n%s", _SEPARATOR)
    logger.debug("[OCR] 1단계: OCR 원본 추출 데이터")
    logger.debug("%s", _SEPARATOR)
    logger.debug("  status     : %s", ocr_result.get("status"))
    logger.debug("  confidence : %s", ocr_result.get("confidence"))
    logger.debug("  총 라인 수 : %d", len(lines))
    logger.debug("  ────────────────────────────────")
    for i, line in enumerate(lines):
        text = line.get("text", "")
        height = line.get("height", "-")
        h_score = line.get("header_score", "-")
        tokens = line.get("token_count", "-")
        logger.debug("  [%03d] h=%s score=%s tok=%s | %s", i, height, h_score, tokens, text)
    logger.debug("%s\n", _SEPARATOR)


def _debug_preprocessed_sections(raw_sections: dict) -> None:
    """Header 기반 구조화 결과 (전처리 후)"""
    logger.debug("\n%s", _SEPARATOR)
    logger.debug("[OCR] 2단계: Header 기반 구조화 (전처리 후)")
    logger.debug("%s", _SEPARATOR)
    logger.debug("  총 섹션 수 : %d", len(raw_sections))
    logger.debug("")

    for idx, (header, lines) in enumerate(raw_sections.items()):
        logger.debug("  ┌─ 섹션 [%d] header: \"%s\"", idx, header)
        logger.debug("  │  라인 수: %d", len(lines))
        for j, line in enumerate(lines):
            logger.debug("  │  [%02d] %s", j, line)
        logger.debug("  └─────────────────────────────────")
        logger.debug("")

    logger.debug("%s\n", _SEPARATOR)


def _debug_validated_sections(raw_sections: dict) -> None:
    """후보정 결과"""
    logger.debug("\n%s", _SEPARATOR)
    logger.debug("[OCR] 2.5단계: 섹션 후보정 결과")
    logger.debug("%s", _SEPARATOR)
    logger.debug("  총 섹션 수 : %d", len(raw_sections))
    logger.debug("")

    for idx, (header, lines) in enumerate(raw_sections.items()):
        logger.debug("  ┌─ 섹션 [%d] header: \"%s\"", idx, header)
        logger.debug("  │  라인 수: %d", len(lines))
        for j, line in enumerate(lines):
            logger.debug("  │  [%02d] %s", j, line)
        logger.debug("  └─────────────────────────────────")
        logger.debug("")

    logger.debug("%s\n", _SEPARATOR)


def _debug_final_canonical(canonical_map: dict) -> None:
    """최종 Canonical 섹션 분리 결과"""
    logger.debug("\n%s", _SEPARATOR)
    logger.debug("[OCR] 3단계: 최종 Canonical 섹션 매핑")
    logger.debug("%s", _SEPARATOR)
    logger.debug("  총 섹션 수 : %d", len(canonical_map))
    logger.debug("")

    for idx, (section_name, content) in enumerate(canonical_map.items()):
        logger.debug("  ┌─ [%d] %s", idx, section_name)
        if isinstance(content, str):
            for line in content.split("\n"):
                logger.debug("  │  %s", line)
        elif isinstance(content, list):
            for j, item in enumerate(content):
                logger.debug("  │  [%02d] %s", j, item)
        else:
            logger.debug("  │  %s", json.dumps(content, ensure_ascii=False, default=str))
        logger.debug("  └─────────────────────────────────")
        logger.debug("")

    logger.debug("%s\n", _SEPARATOR)


class OcrPipeline:
    """
    IMAGE 기반 JD 전처리 파이프라인

    핵심 전략:
    - OCR 단계에서 '구조'를 이미 확보한다
    - Core / Structural 단계는 절대 재실행하지 않는다
    - CanonicalSectionPipeline만 재사용한다
    """

    def __init__(self):
        self.canonical = CanonicalSectionPipeline()
        self.metadata = MetadataPreprocessor()

    def process(self, input: JdPreprocessInput) -> dict:
        if not input.images:
            raise ValueError("images is required for OcrPipeline")

        # 1️⃣ OCR 실행
        ocr_result = process_ocr_input(input.images)

        if ocr_result["status"] == "FAIL":
            raise RuntimeError("OCR failed: confidence too low")

        _debug_ocr_raw(ocr_result)

        # 2️⃣ Header 기반 구조화 (OCR 전용)
        raw_sections = extract_sections_by_header(ocr_result["lines"])

        _debug_preprocessed_sections(raw_sections)

        # 2.5️⃣ 섹션 구조 후보정
        raw_sections = validate_raw_sections(raw_sections)

        _debug_validated_sections(raw_sections)

        if not raw_sections:
            return {
                "ocr": {
                    "status": ocr_result["status"],
                    "confidence": ocr_result["confidence"],
                    "rawText": ocr_result["rawText"],
                },
                "canonical_map": {},
                "document_meta": {
                    "error": "NO_SECTIONS_DETECTED"
                }
            }

        # 3️⃣ OCR → Section 도메인 객체로 변환
        sections = adapt_ocr_sections_to_sections(raw_sections)

        # 4️⃣ Metadata (OCR 원본 기준)
        document_meta = self.metadata.process(
            [line["text"] for line in ocr_result["lines"] if line.get("text")]
        )

        # 5️⃣ Canonical 후처리 (Semantic → Filter → Canonical)
        canonical_map = self.canonical.process(sections)

        _debug_final_canonical(canonical_map)

        # 6️⃣ 최종 결과
        return {
            "ocr": {
                "status": ocr_result["status"],
                "confidence": ocr_result["confidence"],
                "rawText": ocr_result["rawText"],
            },
            "canonical_map": canonical_map,
            "document_meta": document_meta,
        }
