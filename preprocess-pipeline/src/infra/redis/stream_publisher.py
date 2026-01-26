# src/infra/redis/stream_publisher.py

from typing import Dict
import logging
from .redis_client import RedisClient

logger = logging.getLogger(__name__)

class RedisStreamPublisher:
    """
    Redis Stream Publisher

    책임:
    - Stream에 메시지 발행 (XADD)

    비책임:
    - 메시지 의미 ❌
    - 비즈니스 규칙 ❌
    """

    def __init__(self, redis_client: RedisClient):
        self.redis = redis_client.client

    def publish(
            self,
            stream_key: str,
            message: Dict[str, str]
    ) -> str:
        """
        Stream에 메시지 발행

        반환:
        - Redis Stream entry id

        주의:
        - message는 반드시 Map[str, str]
        """
        if not message:
            raise ValueError("message must not be empty")

        try:
            entry_id = self.redis.xadd(stream_key, message)

            return entry_id

        except Exception as e:
            logger.error(
                "Redis stream publish failed | stream=%s | message=%s",
                stream_key,
                message,
                exc_info=True
            )
            raise
