# src/main_kafka.py

"""
Kafka 기반 Preprocess Pipeline Entry Point

3개의 Worker (TEXT, OCR, URL)를 threading으로 병렬 실행
각 Worker는 독립적인 Consumer로 각자의 Topic을 소비
단일 Producer를 공유하여 결과를 response topic으로 발행

실패 처리:
- 처리 실패 → fail 토픽 발행
- fail 토픽 발행 실패 → 로컬 파일 백업
- 무조건 commit (파이프라인 멈추지 않음)

Graceful Shutdown:
- SIGTERM/SIGINT 수신 시 모든 Worker 종료 요청
- 각 Worker는 현재 처리 중인 메시지 완료 후 종료
"""

import logging
import signal
import threading
import time
import uuid
from typing import List

from infra.config.kafka_config import (
    load_kafka_config,
    load_kafka_worker_config,
    KafkaConfig,
)
from infra.kafka.kafka_consumer import KafkaStreamConsumer
from infra.kafka.kafka_producer import KafkaStreamProducer
from worker.text_kafka_worker import TextKafkaWorker
from worker.ocr_kafka_worker import OcrKafkaWorker
from worker.url_kafka_worker import UrlKafkaWorker
from worker.base_kafka_worker import BaseKafkaWorker

# ==================================================
# Logging 설정
# ==================================================
from utils.logger import setup_logging

# 환경변수 LOG_LEVEL로 제어 (DEBUG / INFO / WARNING / ERROR)
# 개발: LOG_LEVEL=DEBUG python main_kafka.py
# 운영: LOG_LEVEL=INFO python main_kafka.py (기본값)
setup_logging()

logger = logging.getLogger(__name__)


def create_kafka_consumer(
    kafka_config: KafkaConfig,
    topic: str,
    client_id: str,
) -> KafkaStreamConsumer:
    """Kafka Consumer 생성"""
    return KafkaStreamConsumer(
        bootstrap_servers=kafka_config.bootstrap_servers,
        topic=topic,
        group_id=kafka_config.consumer_group,
        client_id=client_id,
        poll_timeout_sec=kafka_config.poll_timeout_sec,
    )


def create_kafka_producer(
    kafka_config: KafkaConfig,
    client_id: str,
) -> KafkaStreamProducer:
    """Kafka Producer 생성"""
    return KafkaStreamProducer(
        bootstrap_servers=kafka_config.bootstrap_servers,
        client_id=client_id,
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

    # ==================================================
    # 설정 로드
    # ==================================================
    kafka_config = load_kafka_config()
    worker_config = load_kafka_worker_config()

    logger.info(
        "[CONFIG] Kafka: %s",
        kafka_config.bootstrap_servers
    )
    logger.info(
        "[CONFIG] Topics: text=%s ocr=%s url=%s result=%s fail=%s group=%s",
        kafka_config.text_topic,
        kafka_config.ocr_topic,
        kafka_config.url_topic,
        kafka_config.result_topic,
        kafka_config.fail_topic,
        kafka_config.consumer_group,
    )

    # ==================================================
    # Instance 고유 ID
    # ==================================================
    instance_id = uuid.uuid4().hex[:8]
    logger.info("[CONFIG] Instance ID: %s", instance_id)

    # ==================================================
    # 공유 Producer 생성
    # - 모든 Worker가 단일 Producer를 공유
    # - Thread-safe (confluent-kafka 보장)
    # ==================================================
    producer = create_kafka_producer(
        kafka_config=kafka_config,
        client_id=f"producer-{instance_id}",
    )

    # ==================================================
    # Worker 생성
    # ==================================================
    workers: List[BaseKafkaWorker] = []

    # TEXT Worker
    text_consumer = create_kafka_consumer(
        kafka_config=kafka_config,
        topic=kafka_config.text_topic,
        client_id=f"text-consumer-{instance_id}",
    )
    text_worker = TextKafkaWorker(
        consumer=text_consumer,
        producer=producer,
        result_topic=kafka_config.result_topic,
        fail_topic=kafka_config.fail_topic,
        config=worker_config,
    )
    workers.append(text_worker)

    # OCR Worker
    ocr_consumer = create_kafka_consumer(
        kafka_config=kafka_config,
        topic=kafka_config.ocr_topic,
        client_id=f"ocr-consumer-{instance_id}",
    )
    ocr_worker = OcrKafkaWorker(
        consumer=ocr_consumer,
        producer=producer,
        result_topic=kafka_config.result_topic,
        fail_topic=kafka_config.fail_topic,
        config=worker_config,
    )
    workers.append(ocr_worker)

    # URL Worker
    url_consumer = create_kafka_consumer(
        kafka_config=kafka_config,
        topic=kafka_config.url_topic,
        client_id=f"url-consumer-{instance_id}",
    )
    url_worker = UrlKafkaWorker(
        consumer=url_consumer,
        producer=producer,
        result_topic=kafka_config.result_topic,
        fail_topic=kafka_config.fail_topic,
        config=worker_config,
    )
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

    # ==================================================
    # Producer 정리
    # ==================================================
    logger.info("[SHUTDOWN] Flushing producer...")
    producer.close()

    logger.info("=" * 60)
    logger.info("Preprocess Pipeline (Kafka) Stopped")
    logger.info("=" * 60)


if __name__ == "__main__":
    main()
