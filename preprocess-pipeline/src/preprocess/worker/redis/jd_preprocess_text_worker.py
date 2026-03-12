# src/preprocess/worker/jd_preprocess_text_worker.py

import logging
import time

from infra.redis.stream_keys import JdStreamKeys
from inputs.jd_preprocess_input import JdPreprocessInput
from outputs.jd_preprocess_output import JdPreprocessOutput
from preprocess.worker.pipeline.text_preprocess_pipeline import TextPreprocessPipeline
from preprocess.worker.redis.base_jd_preprocess_worker import BaseJdPreprocessWorker

logger = logging.getLogger(__name__)


class JdPreprocessTextWorker(BaseJdPreprocessWorker):
    """
    TEXT 기반 JD 전처리 Worker

    책임:
    - TEXT JD 전처리 파이프라인 실행
    - 전처리 결과를 Output DTO로 변환
    - 결과를 Redis Stream으로 발행 (직렬화는 하위 레이어 책임)

    비책임:
    - Redis payload 구조 정의 ❌
    - 날짜 / JSON 포맷 ❌
    - Consumer 계약 처리 ❌
    """

    def __init__(self):
        super().__init__()
        self.pipeline = TextPreprocessPipeline()

    def process(self, input: JdPreprocessInput) -> JdPreprocessOutput:
        """
        TEXT JD 전처리 실행

        흐름:
        1. 파이프라인 실행
        2. 도메인 결과 추출
        3. Output DTO 생성
        4. Redis Stream 발행
        """
        try:
            # ==================================================
            # 1️⃣ Pipeline 실행
            # - 모든 계산 책임은 Pipeline에 있음
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
            # 3️⃣ Skills 추출 (읽기 전용)
            # ==================================================
            skill_set = (
                document_meta.skill_set
                if document_meta and document_meta.skill_set
                else None
            )

            # ==================================================
            # 4️⃣ Output DTO 구성 (🔥 계약의 기준점)
            # ==================================================
            output = JdPreprocessOutput(
                # Message Meta
                type="JD_PREPROCESS_RESULT",
                message_version="v1",
                created_at=int(time.time() * 1000),

                # Correlation
                request_id=input.request_id,
                brand_name=input.brand_name,
                position_name=input.position_name,
                source=input.source,

                # 🔥 핵심 Canonical 결과
                canonical_map=canonical_map,

                # Recruitment Meta
                recruitment_period_type=period.period_type if period else None,
                recruitment_open_date=period.open_date if period else None,
                recruitment_close_date=period.close_date if period else None,

                # Skills
                skills=skill_set.skills if skill_set else None,
            )

            # ==================================================
            # 5️⃣ Redis Stream 발행
            #
            # - 직렬화 / 포맷 / key 설계는
            #   BaseJdPreprocessWorker 내부에서 처리
            # ==================================================
            self._publish_result(
                output=output,
                stream_key=JdStreamKeys.PREPROCESS_RESPONSE,
            )

            return output

        except Exception:
            # ==================================================
            # 실패 시:
            # - 예외 로깅
            # - 상위 Consumer가 ACK 여부 판단
            # ==================================================
            logger.exception(
                "Text preprocess failed",
                extra={
                    "request_id": input.request_id,
                    "brand_name": input.brand_name,
                    "position_name": input.position_name,
                },
            )
            raise
