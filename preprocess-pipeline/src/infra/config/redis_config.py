import os
from dataclasses import dataclass


@dataclass(frozen=True)
class RedisConfig:
    """Redis 연결 설정"""
    host: str = "localhost"
    port: int = 6379
    db: int = 0


@dataclass(frozen=True)
class WorkerConfig:
    """Worker 동작 설정"""
    # Stream 읽기 설정
    block_ms: int = 2000           # XREADGROUP blocking 시간 (ms)
    count: int = 1                  # 한 번에 읽을 메시지 수

    # Pending sweep 설정
    pending_idle_ms: int = 300000   # 5분 (ms) - pending 메시지 idle 시간
    pending_count: int = 10         # 한 번에 처리할 pending 메시지 수
    sweep_interval: int = 10        # N번 loop마다 pending sweep 실행

    # Graceful shutdown 설정
    shutdown_timeout_sec: int = 30  # shutdown 대기 최대 시간 (초)


@dataclass(frozen=True)
class StreamConfig:
    """Redis Stream 키 설정"""
    # Input Streams (consume) - source별 분리
    text_request: str = "jd:preprocess:text:request:stream"
    ocr_request: str = "jd:preprocess:ocr:request:stream"
    url_request: str = "jd:preprocess:url:request:stream"

    # Output Streams (publish)
    jd_preprocess_result: str = "jd:preprocess:result"

    # Consumer Group
    consumer_group: str = "preprocess-workers"


def load_redis_config() -> RedisConfig:
    """
    환경 변수에서 Redis 설정 로드

    환경 변수:
    - REDIS_HOST (default: localhost)
    - REDIS_PORT (default: 6379)
    - REDIS_DB (default: 0)
    """
    return RedisConfig(
        host=os.environ.get("REDIS_HOST", "localhost"),
        port=int(os.environ.get("REDIS_PORT", "6379")),
        db=int(os.environ.get("REDIS_DB", "0")),
    )


def load_worker_config() -> WorkerConfig:
    """
    환경 변수에서 Worker 설정 로드

    환경 변수:
    - WORKER_BLOCK_MS (default: 2000)
    - WORKER_COUNT (default: 1)
    - WORKER_PENDING_IDLE_MS (default: 300000)
    - WORKER_PENDING_COUNT (default: 10)
    - WORKER_SWEEP_INTERVAL (default: 10)
    - WORKER_SHUTDOWN_TIMEOUT_SEC (default: 30)
    """
    return WorkerConfig(
        block_ms=int(os.environ.get("WORKER_BLOCK_MS", "2000")),
        count=int(os.environ.get("WORKER_COUNT", "1")),
        pending_idle_ms=int(os.environ.get("WORKER_PENDING_IDLE_MS", "300000")),
        pending_count=int(os.environ.get("WORKER_PENDING_COUNT", "10")),
        sweep_interval=int(os.environ.get("WORKER_SWEEP_INTERVAL", "10")),
        shutdown_timeout_sec=int(os.environ.get("WORKER_SHUTDOWN_TIMEOUT_SEC", "30")),
    )


def load_stream_config() -> StreamConfig:
    """
    환경 변수에서 Stream 설정 로드

    환경 변수:
    - STREAM_JD_PREPROCESS_REQUEST (default: jd:preprocess:request)
    - STREAM_JD_PREPROCESS_RESULT (default: jd:preprocess:result)
    - STREAM_CONSUMER_GROUP (default: preprocess-workers)
    """
    return StreamConfig(
        jd_preprocess_request=os.environ.get(
            "STREAM_JD_PREPROCESS_REQUEST", "jd:preprocess:request"
        ),
        jd_preprocess_result=os.environ.get(
            "STREAM_JD_PREPROCESS_RESULT", "jd:preprocess:result"
        ),
        consumer_group=os.environ.get(
            "STREAM_CONSUMER_GROUP", "preprocess-workers"
        ),
    )
