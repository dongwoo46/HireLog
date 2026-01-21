# src/infra/redis/redis_client.py

import redis


class RedisClient:
    """
    Redis Client Wrapper

    책임:
    - Redis 연결 관리
    - redis-py client 제공

    비책임:
    - Stream 처리 로직 ❌
    - 메시지 해석 ❌
    """

    def __init__(
            self,
            host: str = "localhost",
            port: int = 6379,
            db: int = 0,
    ):
        self._client = redis.Redis(
            host=host,
            port=port,
            db=db,
            decode_responses=True,  # 문자열 기반 통신 강제
        )

    @property
    def client(self):
        return self._client
