from inputs.jd_preprocess_input import JdPreprocessInput
from ocr.pipeline import process_ocr_input
from ocr.structure.header_grouping import extract_sections_by_header
from preprocess.worker.pipeline.canonical_section_pipeline import CanonicalSectionPipeline
from preprocess.metadata_preprocess.metadata_preprocessor import MetadataPreprocessor
from preprocess.adapter.ocr_section_adapter import adapt_ocr_sections_to_sections


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

        # 2️⃣ Header 기반 구조화 (OCR 전용)
        raw_sections = extract_sections_by_header(ocr_result["lines"])

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
