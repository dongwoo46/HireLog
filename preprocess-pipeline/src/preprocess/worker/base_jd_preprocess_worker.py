# src/preprocess/worker/base_preprocess_worker.py

import logging

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
            stream_key: str,   # 🔥 호출자가 결정
    ) -> str:
        """
        전처리 결과 publish

        :param output: 전처리 결과 DTO
        :param stream_key: publish 대상 Stream Key
        :return: Redis entry id
        """

        message = RedisStreamSerializer.serialize(
            metadata={
                "type": output.type,
                "requestId": output.request_id,
                "brandName": output.brand_name,
                "positionName": output.position_name,
                "createdAt": str(output.created_at),
                "messageVersion": output.message_version,
            },
            payload={
                "canonicalText": output.canonical_text,
                "source": output.source,
            },
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
