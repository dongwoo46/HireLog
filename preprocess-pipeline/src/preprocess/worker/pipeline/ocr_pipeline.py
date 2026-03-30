import logging
from inputs.jd_preprocess_input import JdPreprocessInput
from domain.job_platform import JobPlatform
from url.preprocessor import preprocess_lines_by_platform
from ocr.pipeline import process_ocr_input
from ocr.structure.header_grouping import extract_sections_by_header
from preprocess.worker.pipeline.canonical_section_pipeline import CanonicalSectionPipeline
from preprocess.metadata_preprocess.metadata_preprocessor import MetadataPreprocessor
from preprocess.adapter.ocr_section_adapter import adapt_ocr_sections_to_sections
from preprocess.post_validation.section_post_validator import validate_raw_sections

logger = logging.getLogger(__name__)


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

        platform = getattr(input, 'platform', JobPlatform.OTHER)

        # 1️⃣ OCR 실행
        ocr_result = process_ocr_input(input.images)

        logger.info(
            "OCR executed",
            extra={
                "status": ocr_result["status"],
                "confidence": round(ocr_result.get("confidence", 0), 2),
                "line_count": len(ocr_result.get("lines", [])),
                "image_count": len(input.images),
            },
        )

        if ocr_result["status"] == "FAIL":
            logger.warning(
                "OCR confidence too low",
                extra={
                    "confidence": round(ocr_result.get("confidence", 0), 2),
                    "image_count": len(input.images),
                },
            )
            raise RuntimeError("OCR failed: confidence too low")

        # 1.5️⃣ Platform 전용 필터 (OCR dict lines 기준)
        ocr_lines = _filter_ocr_lines_by_platform(ocr_result["lines"], platform)

        # 2️⃣ Header 기반 구조화 (OCR 전용)
        raw_sections = extract_sections_by_header(ocr_lines)

        # 2.5️⃣ 섹션 구조 후보정
        raw_sections = validate_raw_sections(raw_sections)

        if not raw_sections:
            logger.warning(
                "No sections detected after OCR",
                extra={
                    "confidence": round(ocr_result.get("confidence", 0), 2),
                    "line_count": len(ocr_result.get("lines", [])),
                },
            )
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


def _filter_ocr_lines_by_platform(lines: list[dict], platform: JobPlatform) -> list[dict]:
    """
    OCR dict lines에 platform 전용 필터 적용

    preprocess_lines_by_platform은 List[str] 기반이므로,
    text 추출 → 필터 → 순서 기반으로 dict lines 복원
    """
    if not lines:
        return lines

    text_strs = [l.get("text", "") for l in lines]
    cleaned_strs = preprocess_lines_by_platform(text_strs, platform)

    # 순서 기반 매핑: cleaned_strs는 원본 순서 보존 부분집합
    result = []
    clean_iter = iter(cleaned_strs)
    next_clean = next(clean_iter, None)

    for line in lines:
        text = line.get("text", "")
        if text == next_clean:
            result.append(line)
            next_clean = next(clean_iter, None)

    return result
