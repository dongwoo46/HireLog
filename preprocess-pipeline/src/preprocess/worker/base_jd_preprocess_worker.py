# src/preprocess/worker/base_preprocess_worker.py

import logging
import json

from outputs.jd_preprocess_output import JdPreprocessOutput
from infra.redis.stream_publisher import RedisStreamPublisher
from infra.redis.redis_client import RedisClient
from infra.redis.stream_serializer import RedisStreamSerializer

logger = logging.getLogger(__name__)


class BaseJdPreprocessWorker:
    """
    JD 전처리 Worker 공통 베이스

    책임:
    - 결과 메시지 직렬화
    - 지정된 Stream으로 publish
    - 성공 로그 포맷 통일

    비책임:
    - TEXT / OCR 판단 ❌
    - Stream 선택 ❌
    """

    def __init__(self):
        redis_client = RedisClient()
        self.publisher = RedisStreamPublisher(redis_client)

    def _publish_result(
            self,
            *,
            output: JdPreprocessOutput,
            stream_key: str,
    ) -> str:
        """
        전처리 결과 publish

        책임:
        - Output DTO → Stream Message 변환
        - metadata / payload 경계 유지
        """

        # ==================================================
        # Payload (비즈니스 데이터)
        # ==================================================
        payload = {
            # 🔥 canonical_text 제거
            # 🔥 canonical_map JSON 직렬화
            "canonicalMap": json.dumps(
                output.canonical_map,
                ensure_ascii=False
            ),
            "source": output.source,
        }

        # ==================================================
        # Recruitment Period (존재하는 경우만 포함)
        # ==================================================
        if output.recruitment_period_type is not None:
            payload["recruitmentPeriodType"] = output.recruitment_period_type

        if output.recruitment_open_date is not None:
            payload["recruitmentOpenDate"] = output.recruitment_open_date

        if output.recruitment_close_date is not None:
            payload["recruitmentCloseDate"] = output.recruitment_close_date

        # ==================================================
        # Stream Message 직렬화
        # ==================================================
        message = RedisStreamSerializer.serialize(
            metadata={
                "type": output.type,
                "requestId": output.request_id,
                "brandName": output.brand_name,
                "positionName": output.position_name,
                "createdAt": str(output.created_at),
                "messageVersion": output.message_version,
            },
            payload=payload,
        )

        entry_id = self.publisher.publish(
            stream_key=stream_key,
            message=message,
        )

        logger.info(
            "[JD_PREPROCESS_PUBLISHED] requestId=%s brand=%s position=%s source=%s stream=%s entryId=%s",
            output.request_id,
            output.brand_name,
            output.position_name,
            output.source,
            stream_key,
            entry_id,
        )

        return entry_id
