# src/worker/base_kafka_worker.py

"""
Kafka 기반 Base Worker

핵심 원칙:
1. Python Worker는 실행기(executor) 역할만 수행
2. 비즈니스 판단, 재시도 정책, 상태 전이는 Java(Spring)에서 담당
3. 어떤 상황에서도 commit은 반드시 수행 (파이프라인 멈추지 않음)
4. producer send 실패로 consumer 흐름이 중단되어선 안 됨

실패 처리 흐름:
1. 처리 실패 → fail 토픽 발행 시도
2. fail 토픽 발행 실패 → 로컬 파일 백업
3. 무조건 commit
"""

import logging
import json
import time
import threading
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Any, Dict, Optional

from infra.config.kafka_config import WorkerConfig
from infra.kafka.kafka_consumer import KafkaStreamConsumer
from infra.kafka.kafka_producer import KafkaStreamProducer
from common.exceptions import ProcessingError, ErrorCode
from outputs.kafka_jd_preprocess_output import KafkaJdPreprocessOutput
from outputs.kafka_fail_output import KafkaFailOutput
from utils.fail_backup import get_fail_backup_writer

logger = logging.getLogger(__name__)


@dataclass
class ProcessContext:
    """처리 컨텍스트 (메시지 처리 중 필요한 정보)"""
    kafka_meta: Dict[str, Any]
    request_id: str
    source: str
    start_time: float
    pipeline_stage: str = "UNKNOWN"


class BaseKafkaWorker(ABC):
    """
    Kafka 기반 Base Worker

    책임:
    - Kafka 메시지 polling
    - process() 호출
    - 성공 시 result 토픽 발행
    - 실패 시 fail 토픽 발행 (발행 실패 시 로컬 백업)
    - 무조건 commit
    - Graceful shutdown

    하위 Worker 책임:
    - process()에서 결과 DTO 반환 또는 ProcessingError 발생
    - publish/commit/offset 관련 로직 없음
    """

    def __init__(
        self,
        consumer: KafkaStreamConsumer,
        producer: KafkaStreamProducer,
        result_topic: str,
        fail_topic: str,
        config: WorkerConfig,
        worker_name: str,
    ):
        self.consumer = consumer
        self.producer = producer
        self.result_topic = result_topic
        self.fail_topic = fail_topic
        self.config = config
        self.worker_name = worker_name
        self._shutdown_requested = threading.Event()
        self._fail_backup = get_fail_backup_writer()

    @abstractmethod
    def process(self, message: Dict[str, Any]) -> KafkaJdPreprocessOutput:
        """
        메시지 처리 (하위 Worker 구현)

        Args:
            message: 파싱된 Kafka 메시지

        Returns:
            KafkaJdPreprocessOutput: 처리 결과

        Raises:
            ProcessingError: 처리 실패 시
        """
        pass

    def run(self):
        """Worker 메인 루프"""
        logger.info("[%s] Worker started", self.worker_name)

        try:
            while not self._shutdown_requested.is_set():
                kafka_msg = self.consumer.poll()

                if kafka_msg is None:
                    continue

                self._handle_message(kafka_msg)

        finally:
            self._cleanup()

    def _handle_message(self, kafka_msg) -> None:
        """
        메시지 처리 + 발행 + commit 일괄 처리

        핵심: 어떤 실패도 commit을 막지 않는다
        """
        context: Optional[ProcessContext] = None

        try:
            # ==================================================
            # 1. 메시지 파싱
            # ==================================================
            message = self._parse_kafka_message(kafka_msg)

            if message is None:
                # JSON 파싱 실패: 재시도 의미 없음 → commit
                return

            context = ProcessContext(
                kafka_meta=message.get("_kafka_meta", {}),
                request_id=message.get("requestId", "unknown"),
                source=self._get_source_from_message(message),
                start_time=time.time(),
                pipeline_stage="MESSAGE_PARSE",
            )

            logger.info(
                "[%s] Processing | offset=%s requestId=%s",
                self.worker_name,
                context.kafka_meta.get("offset", "unknown"),
                context.request_id,
            )

            # ==================================================
            # 2. 전처리 실행
            # ==================================================
            context.pipeline_stage = "PREPROCESS"
            result = self.process(message)

            # ==================================================
            # 3. 성공 → result 토픽 발행
            # ==================================================
            context.pipeline_stage = "PUBLISH_RESULT"
            self._publish_success(result, context)

            logger.info(
                "[%s] Success | requestId=%s",
                self.worker_name,
                context.request_id,
            )

        except ProcessingError as e:
            # ==================================================
            # 4. 처리 실패 → fail 토픽 발행
            # ==================================================
            logger.error(
                "[%s] ProcessingError | requestId=%s errorCode=%s message=%s",
                self.worker_name,
                context.request_id if context else "unknown",
                e.error_code.value,
                e.message,
            )
            self._handle_failure(e, context)

        except Exception as e:
            # ==================================================
            # 5. 예상치 못한 예외 → ProcessingError로 래핑
            # ==================================================
            logger.exception(
                "[%s] Unexpected error | requestId=%s",
                self.worker_name,
                context.request_id if context else "unknown",
            )
            wrapped = ProcessingError(
                error_code=ErrorCode.UNKNOWN_001,
                message=str(e),
                cause=e,
            )
            self._handle_failure(wrapped, context)

        finally:
            # ==================================================
            # 6. 무조건 commit
            # ==================================================
            self._safe_commit(kafka_msg)

    def _publish_success(self, result: KafkaJdPreprocessOutput, context: ProcessContext) -> None:
        """성공 결과 발행"""
        try:
            self.producer.publish(
                topic=self.result_topic,
                message=result.to_dict(),
                key=result.request_id,
            )
            logger.debug(
                "[%s] Published to %s | requestId=%s",
                self.worker_name,
                self.result_topic,
                result.request_id,
            )
        except Exception as e:
            # result 토픽 발행 실패 → fail 토픽으로 전환
            logger.error(
                "[%s] Failed to publish success | requestId=%s error=%s",
                self.worker_name,
                result.request_id,
                str(e),
            )
            wrapped = ProcessingError(
                error_code=ErrorCode.INFRA_KAFKA_001,
                message=f"Result 토픽 발행 실패: {str(e)}",
                cause=e,
            )
            self._handle_failure(wrapped, context)

    def _handle_failure(self, error: ProcessingError, context: Optional[ProcessContext]) -> None:
        """
        실패 처리: fail 토픽 발행 → 실패 시 로컬 백업

        핵심: 예외 전파 없음 (내부 try/except로 흡수)
        """
        if context is None:
            context = ProcessContext(
                kafka_meta={},
                request_id="unknown",
                source="unknown",
                start_time=time.time(),
                pipeline_stage="UNKNOWN",
            )

        # Fail Output 생성
        fail_output = KafkaFailOutput.from_error(
            request_id=context.request_id,
            source=context.source,
            error_code=error.error_code,
            error_message=error.message,
            pipeline_stage=context.pipeline_stage,
            processing_start_time=context.start_time,
            kafka_meta=context.kafka_meta,
        )

        # ==================================================
        # 1차 시도: fail 토픽 발행
        # ==================================================
        publish_error: Optional[str] = None

        try:
            self.producer.publish(
                topic=self.fail_topic,
                message=fail_output.to_dict(),
                key=context.request_id,
            )
            logger.warning(
                "[%s] Published to fail topic | requestId=%s errorCode=%s",
                self.worker_name,
                context.request_id,
                error.error_code.value,
            )
            return  # 성공 시 종료

        except Exception as e:
            publish_error = str(e)
            logger.error(
                "[%s] Failed to publish to fail topic | requestId=%s error=%s",
                self.worker_name,
                context.request_id,
                publish_error,
            )

        # ==================================================
        # 2차 시도: 로컬 백업
        # ==================================================
        self._fail_backup.write(
            request_id=context.request_id,
            source=context.source,
            error_code=error.error_code.value,
            error_message=error.message,
            publish_error=publish_error,
        )

    def _safe_commit(self, kafka_msg) -> None:
        """안전한 commit (실패해도 예외 전파 없음)"""
        try:
            self.consumer.commit(kafka_msg)
            logger.debug(
                "[%s] Committed offset=%s",
                self.worker_name,
                kafka_msg.offset(),
            )
        except Exception as e:
            # commit 실패도 파이프라인을 멈추지 않음
            logger.error(
                "[%s] Commit failed | offset=%s error=%s",
                self.worker_name,
                kafka_msg.offset(),
                str(e),
            )

    def _parse_kafka_message(self, kafka_msg) -> Optional[Dict[str, Any]]:
        """Kafka 메시지 파싱"""
        try:
            message = json.loads(kafka_msg.value().decode("utf-8"))
            message["_kafka_meta"] = {
                "topic": kafka_msg.topic(),
                "partition": kafka_msg.partition(),
                "offset": kafka_msg.offset(),
                "timestamp": kafka_msg.timestamp()[1] if kafka_msg.timestamp() else None,
            }
            return message
        except Exception as e:
            logger.error(
                "[%s] Kafka message parse failed | offset=%s error=%s",
                self.worker_name,
                kafka_msg.offset(),
                str(e),
            )
            return None

    def _get_source_from_message(self, message: Dict[str, Any]) -> str:
        """메시지에서 source 추출"""
        source = message.get("source", "")
        if isinstance(source, str):
            return source.upper()
        return "UNKNOWN"

    def shutdown(self):
        """Graceful shutdown 요청"""
        logger.info("[%s] Shutdown requested", self.worker_name)
        self._shutdown_requested.set()

    def _cleanup(self):
        """리소스 정리"""
        logger.info("[%s] Cleaning up", self.worker_name)
        self.consumer.close()
        logger.info("[%s] Worker stopped", self.worker_name)
