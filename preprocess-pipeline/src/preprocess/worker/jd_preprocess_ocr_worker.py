# src/preprocess/worker/jd_preprocess_ocr_worker.py

import logging
import time

from infra.redis.stream_keys import JdStreamKeys
from inputs.jd_preprocess_input import JdPreprocessInput
from outputs.jd_preprocess_output import JdPreprocessOutput
from preprocess.worker.pipeline.ocr_pipeline import OcrPipeline
from preprocess.worker.base_jd_preprocess_worker import BaseJdPreprocessWorker

logger = logging.getLogger(__name__)


class JdPreprocessOcrWorker(BaseJdPreprocessWorker):
    """
    OCR 기반 JD 전처리 Worker

    역할:
    - OCR 수행
    - Pipeline 실행
    - 결과 DTO 매핑
    - Redis Stream 발행

    주의:
    - TEXT 워커와 출력 계약은 100% 동일해야 한다
    """

    def __init__(self):
        super().__init__()
        logger.info("[OCR_WORKER_INIT] initializing OCR pipeline")
        self.pipeline = OcrPipeline()

    def process(self, input: JdPreprocessInput) -> JdPreprocessOutput:
        try:
            # ==================================================
            # 1️⃣ OCR + Preprocess Pipeline 실행
            # ==================================================
            result = self.pipeline.process(input)

            canonical_map = result["canonical_map"]
            document_meta = result.get("document_meta")

            # ==================================================
            # 2️⃣ Recruitment Period 추출 (읽기 전용)
            # ==================================================
            period = (
                document_meta.recruitment_period
                if document_meta and document_meta.recruitment_period
                else None
            )

            # ==================================================
            # 3️⃣ Output DTO 구성
            # ==================================================
            output = JdPreprocessOutput(
                type="JD_PREPROCESS_RESULT",
                message_version="v1",
                created_at=int(time.time() * 1000),

                request_id=input.request_id,
                brand_name=input.brand_name,
                position_name=input.position_name,
                source=input.source,

                # 🔥 핵심 데이터 (TEXT / OCR 공통)
                canonical_map=canonical_map,

                recruitment_period_type=period.period_type if period else None,
                recruitment_open_date=period.open_date if period else None,
                recruitment_close_date=period.close_date if period else None,
            )

            # ==================================================
            # 4️⃣ Redis Stream 발행
            # ==================================================
            self._publish_result(
                output=output,
                stream_key=JdStreamKeys.PREPROCESS_RESPONSE,
            )

            return output

        except Exception as e:
            logger.exception(
                "[JD_OCR_PREPROCESS_FAILED] requestId=%s brand=%s position=%s error=%s",
                input.request_id,
                input.brand_name,
                input.position_name,
                str(e),
            )
            raise
