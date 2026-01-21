from inputs.jd_preprocess_input import JdPreprocessInput
from preprocess.worker.pipeline.text_preprocess_pipeline import TextPreprocessPipeline

from ocr.pipeline import process_ocr_input


class OcrPipeline:
    """
    IMAGE 기반 JD 전처리 파이프라인

    역할:
    - OCR 파이프라인 호출
    - OCR 결과를 JD 입력으로 변환
    - TextPreprocessPipeline에 위임

    주의:
    - OCR 로직은 절대 여기서 구현하지 않는다
    - 이 클래스는 '조립 테스트 / 운영 연결용'이다
    """

    def __init__(self):
        self.text_pipeline = TextPreprocessPipeline()

    def process(self, input: JdPreprocessInput) -> dict:
        """
        OCR → JD 전처리 전체 흐름 실행
        """

        print("[OcrPipeline] start")

        if not input.image_url:
            raise ValueError("image_url is required for OcrPipeline")

        # ==================================================
        # 1️⃣ OCR 파이프라인 실행
        # ==================================================
        ocr_result = process_ocr_input(input.image_url)

        # OCR 실패 시: 여기서 끊어도 되고, fallback 정책 가능
        if ocr_result["status"] == "FAIL":
            raise RuntimeError("OCR failed: confidence too low")

        # ==================================================
        # 2️⃣ JD 입력 구성
        # ==================================================
        # 우선순위:
        # 1) lines (구조 보존)
        # 2) rawText fallback

        if ocr_result.get("lines"):
            jd_text = "\n".join(ocr_result["lines"])
        else:
            jd_text = ocr_result.get("rawText", "")

        jd_input = JdPreprocessInput(
            text=jd_text,
            image_url=input.image_url,
            source=input.source
        )

        # ==================================================
        # 3️⃣ 기존 Text JD 파이프라인에 위임
        # ==================================================
        jd_result = self.text_pipeline.process(jd_input)

        # ==================================================
        # 4️⃣ OCR 메타 정보 포함해서 반환
        # ==================================================
        return {
            "ocr": {
                "status": ocr_result["status"],
                "confidence": ocr_result["confidence"],
                "rawText": ocr_result["rawText"],
            },
            "jd": jd_result
        }
