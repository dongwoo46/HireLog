# src/infra/redis/redis_client.py

import logging
import redis

logger = logging.getLogger(__name__)


class RedisConnectionError(Exception):
    """Redis 연결 실패 예외"""
    pass


class RedisClient:
    """
    Redis Client Wrapper

    책임:
    - Redis 연결 관리
    - redis-py client 제공
    - 연결 상태 검증

    비책임:
    - Stream 처리 로직 ❌
    - 메시지 해석 ❌
    """

    def __init__(
            self,
            host: str = "localhost",
            port: int = 6379,
            db: int = 0,
            verify_connection: bool = True,
    ):
        self._client = redis.Redis(
            host=host,
            port=port,
            db=db,
            decode_responses=True,  # 문자열 기반 통신 강제
        )
        self._host = host
        self._port = port

        if verify_connection:
            self._verify_connection()

    def _verify_connection(self):
        """
        Redis 연결 상태 검증 (PING)

        Raises:
            RedisConnectionError: 연결 실패 시
        """
        try:
            self._client.ping()
            logger.info(
                "[REDIS_CONNECTED] host=%s port=%d",
                self._host, self._port
            )
        except redis.exceptions.ConnectionError as e:
            raise RedisConnectionError(
                f"Failed to connect to Redis at {self._host}:{self._port}"
            ) from e

    @property
    def client(self):
        return self._client

    def ping(self) -> bool:
        """Redis 연결 상태 확인"""
        try:
            return self._client.ping()
        except redis.exceptions.ConnectionError:
            return False
