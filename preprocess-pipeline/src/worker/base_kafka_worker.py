# src/worker/base_kafka_worker.py

import logging
from abc import ABC, abstractmethod
from typing import Any, Dict, Optional
import threading
import json

from infra.config.kafka_config import WorkerConfig
from infra.kafka.kafka_consumer import KafkaStreamConsumer

logger = logging.getLogger(__name__)


class BaseKafkaWorker(ABC):
    """
    Kafka 기반 Base Worker

    책임:
    - Kafka 메시지 polling
    - process() 호출
    - commit 여부 결정
    - Graceful shutdown
    """

    def __init__(
            self,
            consumer: KafkaStreamConsumer,
            config: WorkerConfig,
            worker_name: str,
    ):
        self.consumer = consumer
        self.config = config
        self.worker_name = worker_name
        self._shutdown_requested = threading.Event()

    @abstractmethod
    def process(self, message: Dict[str, Any]) -> bool:
        """
        Returns:
            True  -> 처리 + 사이드 이펙트 완료 (commit)
            False -> 처리 실패 (재처리)
        """
        pass

    def run(self):
        logger.info("[%s] Worker started", self.worker_name)

        try:
            while not self._shutdown_requested.is_set():
                kafka_msg = self.consumer.poll()

                if kafka_msg is None:
                    continue

                message = self._parse_kafka_message(kafka_msg)

                if message is None:
                    # JSON 파싱 실패는 재시도 의미 없음
                    self.consumer.commit(kafka_msg)
                    continue

                try:
                    success = self.process(message)
                except Exception:
                    logger.exception("[%s] Unhandled exception", self.worker_name)
                    success = False

                if success:
                    self.consumer.commit(kafka_msg)
                    logger.debug(
                        "[%s] Committed offset=%s",
                        self.worker_name,
                        kafka_msg.offset(),
                    )
                else:
                    logger.warning(
                        "[%s] Processing failed, will retry",
                        self.worker_name,
                    )

        finally:
            self._cleanup()

    def _parse_kafka_message(self, kafka_msg) -> Optional[Dict[str, Any]]:
        try:
            message = json.loads(kafka_msg.value().decode("utf-8"))
            message["_kafka_meta"] = {
                "topic": kafka_msg.topic(),
                "partition": kafka_msg.partition(),
                "offset": kafka_msg.offset(),
                "timestamp": kafka_msg.timestamp()[1]
                if kafka_msg.timestamp()
                else None,
            }
            return message
        except Exception:
            logger.exception("[%s] Kafka message parse failed", self.worker_name)
            return None

    def shutdown(self):
        logger.info("[%s] Shutdown requested", self.worker_name)
        self._shutdown_requested.set()

    def _cleanup(self):
        logger.info("[%s] Cleaning up", self.worker_name)
        self.consumer.close()
        logger.info("[%s] Worker stopped", self.worker_name)
