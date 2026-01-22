# src/preprocess/worker/jd_preprocess_text_worker.py

import logging
import time

from infra.redis.stream_keys import JdStreamKeys
from inputs.jd_preprocess_input import JdPreprocessInput
from outputs.jd_preprocess_output import JdPreprocessOutput
from preprocess.worker.pipeline.text_preprocess_pipeline import TextPreprocessPipeline
from preprocess.worker.base_jd_preprocess_worker import BaseJdPreprocessWorker

logger = logging.getLogger(__name__)


class JdPreprocessTextWorker(BaseJdPreprocessWorker):
    """
    TEXT 기반 JD 전처리 Worker

    역할:
    - Pipeline 실행
    - 결과 DTO로 매핑
    - Redis Stream 발행
    """

    def __init__(self):
        super().__init__()
        self.pipeline = TextPreprocessPipeline()

    def process(self, input: JdPreprocessInput) -> JdPreprocessOutput:
        try:
            # ==================================================
            # 1️⃣ Pipeline 실행 (모든 계산은 여기서 끝)
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

                # 🔥 핵심 데이터
                canonical_map=canonical_map,

                # 메타 정보
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
                "[JD_TEXT_PREPROCESS_FAILED] requestId=%s brand=%s position=%s error=%s",
                input.request_id,
                input.brand_name,
                input.position_name,
                str(e),
            )
            raise
