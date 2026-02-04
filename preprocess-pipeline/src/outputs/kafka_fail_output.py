# src/outputs/kafka_fail_output.py

"""
Kafka 실패 응답 DTO

jd.preprocess.response.fail 토픽으로 발행되는 메시지 구조
원본 메시지는 절대 포함하지 않는다.
"""

import socket
import time
import uuid
from dataclasses import dataclass
from typing import Any, Dict, Optional

from common.exceptions import ErrorCode, get_error_category


def _get_hostname() -> str:
    """호스트명 조회 (실패 시 unknown)"""
    try:
        return socket.gethostname()
    except Exception:
        return "unknown"


@dataclass
class KafkaFailOutput:
    """
    JD 전처리 실패 응답 Kafka Event

    Spring의 JdPreprocessFailEvent와 1:1 매핑

    포함 필드:
    - requestId: 요청 추적용
    - source: 소스 타입 (OCR/TEXT/URL)
    - errorCode: 에러 분류
    - errorMessage: 간단한 에러 설명
    - errorCategory: 재시도 가능 여부 힌트
    - pipelineStage: 실패 발생 단계
    - workerHost: 장애 노드 추적용
    - kafkaMetadata: 원본 메시지 위치 (재처리 시 필요)
    - occurredAt: 발생 시간
    - processingDurationMs: 처리 소요 시간
    """

    # Event Identity
    event_id: str
    request_id: str

    # Event Metadata
    event_type: str  # "JD_PREPROCESS_FAILED"
    version: str  # "v1"
    occurred_at: int  # Unix timestamp (ms)

    # Error Info
    source: str
    error_code: str
    error_message: str
    error_category: str  # RECOVERABLE / PERMANENT / UNKNOWN
    pipeline_stage: str

    # Debug Info
    worker_host: str
    processing_duration_ms: int

    # Kafka Metadata (원본 메시지 위치)
    original_topic: Optional[str] = None
    original_partition: Optional[int] = None
    original_offset: Optional[int] = None

    def to_dict(self) -> Dict[str, Any]:
        """
        Kafka 메시지로 직렬화 (camelCase)
        """
        return {
            # Event Identity
            "eventId": self.event_id,
            "requestId": self.request_id,

            # Event Metadata
            "eventType": self.event_type,
            "version": self.version,
            "occurredAt": self.occurred_at,

            # Error Info
            "source": self.source,
            "errorCode": self.error_code,
            "errorMessage": self.error_message,
            "errorCategory": self.error_category,
            "pipelineStage": self.pipeline_stage,

            # Debug Info
            "workerHost": self.worker_host,
            "processingDurationMs": self.processing_duration_ms,

            # Kafka Metadata
            "kafkaMetadata": {
                "originalTopic": self.original_topic,
                "originalPartition": self.original_partition,
                "originalOffset": self.original_offset,
            },
        }

    @staticmethod
    def from_error(
        request_id: str,
        source: str,
        error_code: ErrorCode,
        error_message: str,
        pipeline_stage: str,
        processing_start_time: float,
        kafka_meta: Optional[Dict[str, Any]] = None,
    ) -> "KafkaFailOutput":
        """
        ProcessingError로부터 KafkaFailOutput 생성

        Args:
            request_id: 요청 ID
            source: 소스 타입 (OCR/TEXT/URL)
            error_code: ErrorCode Enum
            error_message: 에러 메시지
            pipeline_stage: 실패 발생 단계
            processing_start_time: 처리 시작 시간 (time.time())
            kafka_meta: 원본 Kafka 메시지 메타데이터
        """
        now = time.time()
        duration_ms = int((now - processing_start_time) * 1000)

        kafka_meta = kafka_meta or {}

        return KafkaFailOutput(
            # Event Identity
            event_id=str(uuid.uuid4()),
            request_id=request_id,

            # Event Metadata
            event_type="JD_PREPROCESS_FAILED",
            version="v1",
            occurred_at=int(now * 1000),

            # Error Info
            source=source,
            error_code=error_code.value,
            error_message=error_message,
            error_category=get_error_category(error_code).value,
            pipeline_stage=pipeline_stage,

            # Debug Info
            worker_host=_get_hostname(),
            processing_duration_ms=duration_ms,

            # Kafka Metadata
            original_topic=kafka_meta.get("topic"),
            original_partition=kafka_meta.get("partition"),
            original_offset=kafka_meta.get("offset"),
        )
