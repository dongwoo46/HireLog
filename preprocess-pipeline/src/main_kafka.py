# src/main_kafka.py

import logging
import signal
import sys
import threading
import time
import uuid
from typing import List

from infra.config.kafka_config import load_kafka_config, load_kafka_worker_config
from infra.kafka.kafka_consumer import KafkaStreamConsumer
from worker.text_kafka_worker import TextKafkaWorker
from worker.base_kafka_worker import BaseKafkaWorker

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)]
)

logger = logging.getLogger(__name__)


def create_kafka_consumer(
        bootstrap_servers: str,
        topic: str,
        group_id: str,
        client_id: str,
        poll_timeout_sec: float,
) -> KafkaStreamConsumer:
    """Kafka Consumer 생성"""
    return KafkaStreamConsumer(
        bootstrap_servers=bootstrap_servers,
        topic=topic,
        group_id=group_id,
        client_id=client_id,
        poll_timeout_sec=poll_timeout_sec,
    )


def run_worker(worker: BaseKafkaWorker):
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
    logger.info("Preprocess Pipeline (Kafka) Starting...")
    logger.info("=" * 60)

    # 설정 로드
    kafka_config = load_kafka_config()
    worker_config = load_kafka_worker_config()

    logger.info(
        "[CONFIG] Kafka: %s",
        kafka_config.bootstrap_servers
    )
    logger.info(
        "[CONFIG] Topics: text=%s ocr=%s url=%s group=%s",
        kafka_config.text_topic,
        kafka_config.ocr_topic,
        kafka_config.url_topic,
        kafka_config.consumer_group,
    )

    # Consumer 고유 이름
    instance_id = uuid.uuid4().hex[:8]

    # Worker 생성
    workers: List[BaseKafkaWorker] = []

    # TEXT Worker
    text_consumer = create_kafka_consumer(
        bootstrap_servers=kafka_config.bootstrap_servers,
        topic=kafka_config.text_topic,
        group_id=kafka_config.consumer_group,
        client_id=f"text-{instance_id}",
        poll_timeout_sec=kafka_config.poll_timeout_sec,
    )
    text_worker = TextKafkaWorker(text_consumer, worker_config)
    workers.append(text_worker)

    # OCR Worker (동일 구조)
    # URL Worker (동일 구조)

    # Shutdown 핸들러
    shutdown_event = threading.Event()

    def shutdown_handler(signum, frame):
        sig_name = signal.Signals(signum).name
        logger.info("[SHUTDOWN] Received %s", sig_name)
        shutdown_event.set()
        for worker in workers:
            worker.shutdown()

    signal.signal(signal.SIGTERM, shutdown_handler)
    signal.signal(signal.SIGINT, shutdown_handler)

    # Worker 스레드 시작
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
        logger.info("[STARTED] %s", worker.worker_name)

    logger.info("=" * 60)
    logger.info("All workers running. Press Ctrl+C to shutdown.")
    logger.info("=" * 60)

    # 메인 스레드 대기
    try:
        while not shutdown_event.is_set():
            alive_count = sum(1 for t in threads if t.is_alive())
            if alive_count == 0:
                logger.warning("[MAIN] All workers stopped")
                break
            time.sleep(1)
    except KeyboardInterrupt:
        logger.info("[MAIN] KeyboardInterrupt")
        shutdown_event.set()
        for worker in workers:
            worker.shutdown()

    # 종료 대기
    logger.info("[SHUTDOWN] Waiting for workers...")
    for thread in threads:
        thread.join(timeout=worker_config.shutdown_timeout_sec)
        if thread.is_alive():
            logger.warning("[SHUTDOWN] %s timeout", thread.name)
        else:
            logger.info("[SHUTDOWN] %s finished", thread.name)

    logger.info("=" * 60)
    logger.info("Pipeline Stopped")
    logger.info("=" * 60)


if __name__ == "__main__":
    main()