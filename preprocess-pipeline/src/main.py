# src/main.py

"""
Preprocess Pipeline Main Entry Point

3개의 Worker (TEXT, OCR, URL)를 threading으로 병렬 실행
각 Worker는 독립적인 Consumer로 동일 Stream을 소비

Graceful Shutdown:
- SIGTERM/SIGINT 수신 시 모든 Worker 종료 요청
- 각 Worker는 현재 처리 중인 메시지 완료 후 종료
"""

import logging
import signal
import sys
import threading
import time
import uuid
from typing import List

from config import (
    load_redis_config,
    load_worker_config,
    load_stream_config,
)
from infra.redis.redis_client import RedisClient, RedisConnectionError
from infra.redis.stream_consumer import RedisStreamConsumer
from worker.text_stream_worker import TextStreamWorker
from worker.ocr_stream_worker import OcrStreamWorker
from worker.url_stream_worker import UrlStreamWorker
from worker.base_worker import BaseWorker

# ==================================================
# Logging 설정
# ==================================================
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout),
    ]
)

logger = logging.getLogger(__name__)


def create_consumer(
    redis_client: RedisClient,
    stream_key: str,
    group: str,
    consumer_name: str,
    worker_config,
) -> RedisStreamConsumer:
    """Consumer 생성"""
    return RedisStreamConsumer(
        redis_client=redis_client,
        stream_key=stream_key,
        group=group,
        consumer_name=consumer_name,
        block_ms=worker_config.block_ms,
        count=worker_config.count,
    )


def run_worker(worker: BaseWorker):
    """Worker 실행 (스레드 타겟)"""
    try:
        worker.run()
    except Exception as e:
        logger.error(
            "[WORKER_CRASH] worker=%s error=%s",
            worker.worker_name, str(e),
            exc_info=True
        )


def main():
    """메인 진입점"""
    logger.info("=" * 60)
    logger.info("Preprocess Pipeline Starting...")
    logger.info("=" * 60)

    # ==================================================
    # 설정 로드
    # ==================================================
    redis_config = load_redis_config()
    worker_config = load_worker_config()
    stream_config = load_stream_config()

    logger.info(
        "[CONFIG] Redis: %s:%d db=%d",
        redis_config.host, redis_config.port, redis_config.db
    )
    logger.info(
        "[CONFIG] Stream: text=%s ocr=%s url=%s group=%s",
        stream_config.text_request,
        stream_config.ocr_request,
        stream_config.url_request,
        stream_config.consumer_group,
    )
    logger.info(
        "[CONFIG] Worker: block_ms=%d pending_idle_ms=%d sweep_interval=%d",
        worker_config.block_ms,
        worker_config.pending_idle_ms,
        worker_config.sweep_interval,
    )

    # ==================================================
    # Redis 연결
    # ==================================================
    try:
        redis_client = RedisClient(
            host=redis_config.host,
            port=redis_config.port,
            db=redis_config.db,
        )
    except RedisConnectionError as e:
        logger.error("[STARTUP_FAILED] Redis connection failed: %s", str(e))
        sys.exit(1)

    # ==================================================
    # Consumer 고유 이름 생성
    # ==================================================
    instance_id = uuid.uuid4().hex[:8]

    # ==================================================
    # Worker 생성
    # ==================================================
    workers: List[BaseWorker] = []

    # TEXT Worker
    text_consumer = create_consumer(
        redis_client=redis_client,
        stream_key=stream_config.text_request,
        group=stream_config.consumer_group,
        consumer_name=f"text-{instance_id}",
        worker_config=worker_config,
    )
    text_worker = TextStreamWorker(text_consumer, worker_config)
    workers.append(text_worker)

    # OCR Worker
    ocr_consumer = create_consumer(
        redis_client=redis_client,
        stream_key=stream_config.ocr_request,
        group=stream_config.consumer_group,
        consumer_name=f"ocr-{instance_id}",
        worker_config=worker_config,
    )
    ocr_worker = OcrStreamWorker(ocr_consumer, worker_config)
    workers.append(ocr_worker)

    # URL Worker
    url_consumer = create_consumer(
        redis_client=redis_client,
        stream_key=stream_config.url_request,
        group=stream_config.consumer_group,
        consumer_name=f"url-{instance_id}",
        worker_config=worker_config,
    )
    url_worker = UrlStreamWorker(url_consumer, worker_config)
    workers.append(url_worker)

    # ==================================================
    # Shutdown 핸들러
    # ==================================================
    shutdown_event = threading.Event()

    def shutdown_handler(signum, frame):
        sig_name = signal.Signals(signum).name
        logger.info("[SHUTDOWN] Received %s, initiating graceful shutdown...", sig_name)
        shutdown_event.set()

        for worker in workers:
            worker.shutdown()

    signal.signal(signal.SIGTERM, shutdown_handler)
    signal.signal(signal.SIGINT, shutdown_handler)

    # ==================================================
    # Worker 스레드 시작
    # ==================================================
    threads: List[threading.Thread] = []

    for worker in workers:
        thread = threading.Thread(
            target=run_worker,
            args=(worker,),
            name=worker.worker_name,
            daemon=False,
        )
        thread.start()
        threads.append(thread)
        logger.info("[STARTED] %s thread started", worker.worker_name)

    logger.info("=" * 60)
    logger.info("All workers running. Press Ctrl+C to shutdown.")
    logger.info("=" * 60)

    # ==================================================
    # 메인 스레드 대기
    # ==================================================
    try:
        while not shutdown_event.is_set():
            alive_count = sum(1 for t in threads if t.is_alive())
            if alive_count == 0:
                logger.warning("[MAIN] All worker threads have stopped")
                break

            time.sleep(1)

    except KeyboardInterrupt:
        logger.info("[MAIN] KeyboardInterrupt received")
        shutdown_event.set()
        for worker in workers:
            worker.shutdown()

    # ==================================================
    # 스레드 종료 대기
    # ==================================================
    logger.info("[SHUTDOWN] Waiting for worker threads to finish...")

    for thread in threads:
        thread.join(timeout=worker_config.shutdown_timeout_sec)
        if thread.is_alive():
            logger.warning("[SHUTDOWN] Thread %s did not finish in time", thread.name)
        else:
            logger.info("[SHUTDOWN] Thread %s finished", thread.name)

    logger.info("=" * 60)
    logger.info("Preprocess Pipeline Stopped")
    logger.info("=" * 60)


if __name__ == "__main__":
    main()
