# src/inputs/parse_jd_preprocess_message.py

import logging
from typing import Any, Dict

from domain.job_source import JobSource
from inputs.kafka_jd_preprocess_input import KafkaJdPreprocessInput

logger = logging.getLogger(__name__)


class MessageParseError(Exception):
    """메시지 파싱 실패 예외"""
    pass


def parse_kafka_jd_preprocess_message(message: Dict[str, Any]) -> KafkaJdPreprocessInput:
    """
    Kafka 메시지를 KafkaJdPreprocessInput으로 변환

    Spring에서 전송하는 JdPreprocessRequestMessage 구조:
    {
        "eventId": "uuid",
        "requestId": "uuid",
        "occurredAt": 1234567890,
        "version": "1.0",
        "brandName": "회사명",
        "positionName": "직무명",
        "source": "TEXT" | "IMAGE" | "URL",
        "text": "공고 내용",
        "_kafka_meta": {  # BaseKafkaWorker가 추가
            "topic": "...",
            "partition": 0,
            "offset": 123,
            "timestamp": 1234567890
        }
    }

    Args:
        message: Kafka 메시지 dict

    Returns:
        KafkaJdPreprocessInput 객체

    Raises:
        MessageParseError: 필수 필드 누락 또는 형식 오류
    """
    try:
        # 필수 필드 검증
        required_fields = [
            "requestId",
            "brandName",
            "positionName",
            "source",
            "text",
        ]

        missing_fields = [f for f in required_fields if f not in message]
        if missing_fields:
            raise MessageParseError(
                f"Missing required fields: {', '.join(missing_fields)}"
            )

        # source enum 변환
        source_str = message["source"].upper()
        try:
            source = JobSource[source_str]
        except KeyError:
            raise MessageParseError(
                f"Invalid source type: {source_str}. "
                f"Expected one of: {[s.name for s in JobSource]}"
            )

        # KafkaJdPreprocessInput 생성
        return KafkaJdPreprocessInput(
            request_id=message["requestId"],
            brand_name=message["brandName"],
            position_name=message["positionName"],
            source=source,
            text=message["text"],
            event_id=message.get("eventId"),
            occurred_at=message.get("occurredAt"),
            version=message.get("version", "1.0"),
        )

    except MessageParseError:
        raise
    except Exception as e:
        logger.error(
            "Unexpected error parsing Kafka message | error=%s message=%s",
            str(e),
            message,
            exc_info=True,
        )
        raise MessageParseError(f"Failed to parse message: {str(e)}") from e