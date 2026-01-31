# src/worker/base_worker.py

import logging
import threading
from abc import ABC, abstractmethod
from typing import Any, Dict

from infra.redis.stream_consumer import RedisStreamConsumer
from infra.config.redis_config import WorkerConfig

logger = logging.getLogger(__name__)


class BaseWorker(ABC):
    """
    Worker 메인 루프 베이스 클래스

    책임:
    - XREADGROUP 기반 메시지 수신 루프
    - 메시지 처리 + 성공 시 ACK
    - Pending 메시지 주기적 sweep
    - Graceful shutdown

    비책임:
    - 메시지 파싱 / 비즈니스 로직 (서브클래스 구현)
    """

    def __init__(
        self,
        consumer: RedisStreamConsumer,
        config: WorkerConfig,
        worker_name: str = "worker",
    ):
        self.consumer = consumer
        self.config = config
        self.worker_name = worker_name

        # Shutdown 제어
        self._shutdown_event = threading.Event()
        self._processing = threading.Event()

        # Loop 카운터 (pending sweep 주기 계산용)
        self._loop_count = 0

    # ==================================================
    # Abstract Methods (서브클래스 구현 필수)
    # ==================================================

    @abstractmethod
    def process(self, message: Dict[str, Any]) -> bool:
        """
        메시지 처리 로직

        Args:
            message: Redis Stream 메시지 {"id": ..., "values": {...}}

        Returns:
            True: 처리 성공 → ACK
            False: 처리 실패 → ACK 스킵 (pending 유지)

        Raises:
            Exception: 예외 발생 시에도 ACK 스킵
        """
        pass

    # ==================================================
    # Main Loop
    # ==================================================

    def run(self):
        """
        메인 루프 실행

        1. 메시지 수신 → 처리 → ACK 반복
        2. 주기적 pending sweep
        3. shutdown 요청 시 graceful 종료
        """
        logger.info(
            "[%s] Worker started | stream=%s group=%s consumer=%s",
            self.worker_name,
            self.consumer.stream_key,
            self.consumer.group,
            self.consumer.consumer_name,
        )

        # 시작 시 pending sweep 실행
        self._sweep_pending()

        while not self._shutdown_event.is_set():
            try:
                self._loop_count += 1

                # 1. 메시지 읽기
                messages = self.consumer.read()

                # 2. 메시지 처리
                for message in messages:
                    self._process_message(message)

                # 3. 주기적 pending sweep
                if self._loop_count % self.config.sweep_interval == 0:
                    self._sweep_pending()

            except Exception as e:
                logger.error(
                    "[%s] Loop error: %s",
                    self.worker_name, str(e),
                    exc_info=True
                )

        logger.info("[%s] Worker stopped", self.worker_name)

    def _process_message(self, message: Dict[str, Any]):
        """
        단일 메시지 처리 + ACK

        처리 성공 시에만 ACK.
        실패 시 ACK 스킵하여 pending 유지.
        """
        message_id = message.get("id", "unknown")

        logger.info(
            "[%s] Processing message | id=%s",
            self.worker_name, message_id
        )

        self._processing.set()

        try:
            success = self.process(message)

            if success:
                self.consumer.ack(message_id)
                logger.info(
                    "[%s] Message ACKed | id=%s",
                    self.worker_name, message_id
                )
            else:
                logger.warning(
                    "[%s] Message processing failed, skipping ACK | id=%s",
                    self.worker_name, message_id
                )

        except Exception as e:
            logger.error(
                "[%s] Message processing error, skipping ACK | id=%s error=%s",
                self.worker_name, message_id, str(e),
                exc_info=True
            )

        finally:
            self._processing.clear()

    # ==================================================
    # Pending Sweep
    # ==================================================

    def _sweep_pending(self):
        """
        Pending 메시지 sweep

        idle time >= pending_idle_ms인 메시지를 XCLAIM으로 획득하여 재처리
        """
        try:
            pending_messages = self.consumer.read_pending(
                min_idle_ms=self.config.pending_idle_ms,
                count=self.config.pending_count,
            )

            if not pending_messages:
                return

            logger.info(
                "[%s] Sweeping %d pending messages",
                self.worker_name, len(pending_messages)
            )

            for message in pending_messages:
                if self._shutdown_event.is_set():
                    break
                self._process_message(message)

        except Exception as e:
            logger.error(
                "[%s] Pending sweep error: %s",
                self.worker_name, str(e),
                exc_info=True
            )

    # ==================================================
    # Graceful Shutdown
    # ==================================================

    def shutdown(self):
        """
        Graceful shutdown 요청

        현재 처리 중인 메시지가 있으면 완료 대기 후 종료
        """
        self._shutdown_event.set()

        if self._processing.is_set():
            logger.info(
                "[%s] Waiting for current message processing to complete...",
                self.worker_name
            )
            self._processing.wait(timeout=self.config.shutdown_timeout_sec)

    def is_running(self) -> bool:
        """Worker 실행 중 여부"""
        return not self._shutdown_event.is_set()
