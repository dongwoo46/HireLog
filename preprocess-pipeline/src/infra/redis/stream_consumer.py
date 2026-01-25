# src/infra/redis/stream_consumer.py

from typing import List, Dict, Any
import redis
from .redis_client import RedisClient


class RedisStreamConsumer:
    """
    Redis Stream Consumer

    책임:
    - XREADGROUP 기반 메시지 수신
    - Consumer Group 관리
    - ACK 처리

    비책임:
    - 메시지 의미 해석 ❌
    - 비즈니스 로직 ❌
    """

    def __init__(
            self,
            redis_client: RedisClient,
            stream_key: str,
            group: str,
            consumer_name: str,
            block_ms: int = 2000,
            count: int = 1,
    ):
        self.redis = redis_client.client
        self.stream_key = stream_key
        self.group = group
        self.consumer_name = consumer_name
        self.block_ms = block_ms
        self.count = count

        self._create_group_if_not_exists()

    def _create_group_if_not_exists(self):
        """
        Consumer Group 생성 (멱등)
        """
        try:
            self.redis.xgroup_create(
                name=self.stream_key,
                groupname=self.group,
                id="0",
                mkstream=True,
            )
        except redis.exceptions.ResponseError:
            # BUSYGROUP: 이미 존재
            pass

    def read(self) -> List[Dict[str, Any]]:
        """
        Stream 메시지 읽기

        반환 형식:
        [
            {
                "id": "1690000000000-0",
                "values": {...}
            }
        ]
        """
        streams = self.redis.xreadgroup(
            groupname=self.group,
            consumername=self.consumer_name,
            streams={self.stream_key: ">"},
            count=self.count,
            block=self.block_ms,
        )

        messages = []

        for _, records in streams:
            for record_id, values in records:
                messages.append({
                    "id": record_id,
                    "values": values,
                })

        return messages

    def ack(self, message_id: str):
        """
        메시지 ACK
        """
        self.redis.xack(self.stream_key, self.group, message_id)
