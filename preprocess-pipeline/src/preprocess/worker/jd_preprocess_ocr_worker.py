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

    특징:
    - Paddle / OCR 모델 로딩
    - TEXT 워커와 완전 격리
    """

    def __init__(self):
        super().__init__()
        logger.info("[OCR_WORKER_INIT] initializing OCR pipeline")
        self.pipeline = OcrPipeline()

    def process(self, input: JdPreprocessInput) -> JdPreprocessOutput:
        try:
            result = self.pipeline.process(input)

            output = JdPreprocessOutput(
                type="JD_PREPROCESS_RESULT",
                message_version="v1",
                created_at=int(time.time() * 1000),

                request_id=input.request_id,
                brand_name=input.brand_name,
                position_name=input.position_name,
                source=input.source,

                canonical_text=result["canonical_text"],
            )

            self._publish_result(
                output=output,
                stream_key=JdStreamKeys.PREPROCESS_OCR_RESPONSE,
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
