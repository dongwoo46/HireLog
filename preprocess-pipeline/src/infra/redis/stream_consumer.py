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

        # timeout 또는 빈 결과 처리
        if not streams:
            return []

        messages = []

        for _, records in streams:
            for record_id, values in records:
                messages.append({
                    "id": record_id,
                    "values": values,
                })

        return messages

    def read_pending(self, min_idle_ms: int = 300000, count: int = 10) -> List[Dict[str, Any]]:
        """
        Pending 메시지 조회 및 소유권 획득 (XPENDING + XCLAIM)

        Args:
            min_idle_ms: 최소 idle 시간 (기본 5분 = 300,000ms)
            count: 조회할 최대 메시지 수

        Returns:
            XCLAIM으로 획득한 메시지 리스트
        """
        # 1. XPENDING으로 pending 메시지 ID 조회
        pending_info = self.redis.xpending_range(
            name=self.stream_key,
            groupname=self.group,
            min="-",
            max="+",
            count=count,
            idle=min_idle_ms,
        )

        if not pending_info:
            return []

        # 2. pending 메시지 ID 추출
        message_ids = [info["message_id"] for info in pending_info]

        if not message_ids:
            return []

        # 3. XCLAIM으로 소유권 획득
        claimed = self.redis.xclaim(
            name=self.stream_key,
            groupname=self.group,
            consumername=self.consumer_name,
            min_idle_time=min_idle_ms,
            message_ids=message_ids,
        )

        if not claimed:
            return []

        # 4. 결과 변환
        messages = []
        for record_id, values in claimed:
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
