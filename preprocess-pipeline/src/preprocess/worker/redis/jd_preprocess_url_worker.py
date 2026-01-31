import logging
import time

from infra.redis.stream_keys import JdStreamKeys
from inputs.jd_preprocess_input import JdPreprocessInput
from outputs.jd_preprocess_output import JdPreprocessOutput
from preprocess.worker.pipeline.url_pipeline import UrlPipeline
from preprocess.worker.redis.base_jd_preprocess_worker import BaseJdPreprocessWorker

logger = logging.getLogger(__name__)


class JdPreprocessUrlWorker(BaseJdPreprocessWorker):
    """
    URL 기반 JD 전처리 Worker

    책임:
    - URL Preprocess 파이프라인 실행
    - 결과 DTO 매핑
    - Redis Stream 발행
    """

    def __init__(self):
        super().__init__()
        self.pipeline = UrlPipeline()

    def process(self, input: JdPreprocessInput) -> JdPreprocessOutput:
        try:
            # ==================================================
            # 1️⃣ URL Pipeline 실행 (OCR 방식 새 파이프라인)
            # ==================================================
            result = self.pipeline.process(input)

            # 새 파이프라인은 직접 canonical_map, document_meta 반환
            canonical_map = result.get("canonical_map", {})
            document_meta = result.get("document_meta")

            # ==================================================
            # 2️⃣ Meta Data 추출
            # ==================================================
            period = (
                document_meta.recruitment_period
                if document_meta and document_meta.recruitment_period
                else None
            )

            skill_set = (
                document_meta.skill_set
                if document_meta and document_meta.skill_set
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

                canonical_map=canonical_map,

                recruitment_period_type=period.period_type if period else None,
                recruitment_open_date=period.open_date if period else None,
                recruitment_close_date=period.close_date if period else None,

                skills=skill_set.skills if skill_set else None,
            )

            # ==================================================
            # 4️⃣ Redis Stream 발행
            # ==================================================
            self._publish_result(
                output=output,
                stream_key=JdStreamKeys.PREPROCESS_RESPONSE,
            )
            
            # 요약용 스트림에도 발행 (요구사항 반영)
            # "streamKey 저장소에 jd:summary:url:request:stream... 만들어줘야해"
            # 근데 URL worker가 소비하는 게 jd:preprocess:url:request:stream 이고
            # 결과는 preprocess response로 나감.
            # summary stream이 별도로 있다면 거기에 쏠 수도 있음.
            # 현재는 표준 응답만 처리. 
            
            return output

        except Exception as e:
            logger.exception(
                "[JD_URL_PREPROCESS_FAILED] requestId=%s brand=%s url=%s error=%s",
                input.request_id,
                input.brand_name,
                input.url,
                str(e),
            )
            raise
