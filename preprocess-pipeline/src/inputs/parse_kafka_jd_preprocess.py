# src/inputs/parse_kafka_jd_preprocess.py

import logging
from typing import Any, Dict, List, Optional

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
        "text": "공고 내용",           # TEXT 소스
        "images": ["path1", "path2"],  # IMAGE 소스
        "url": "https://...",          # URL 소스
        "_kafka_meta": {
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
        # 공통 필수 필드 검증
        common_required = ["requestId", "brandName", "positionName", "source"]
        missing = [f for f in common_required if f not in message]
        if missing:
            raise MessageParseError(f"Missing required fields: {', '.join(missing)}")

        # source enum 변환
        source_str = message["source"].upper()
        try:
            source = JobSource[source_str]
        except KeyError:
            raise MessageParseError(
                f"Invalid source type: {source_str}. "
                f"Expected one of: {[s.name for s in JobSource]}"
            )

        # 소스별 필수 필드 검증
        text: Optional[str] = None
        images: List[str] = []
        url: Optional[str] = None

        if source == JobSource.TEXT:
            text = message.get("text")
            if not text:
                raise MessageParseError("'text' is required when source=TEXT")

        elif source == JobSource.IMAGE:
            raw_images = message.get("images")
            if not raw_images:
                raise MessageParseError("'images' is required when source=IMAGE")
            if isinstance(raw_images, str):
                images = [raw_images]
            elif isinstance(raw_images, list):
                images = raw_images
            else:
                raise MessageParseError(
                    f"'images' must be string or list, got {type(raw_images).__name__}"
                )

        elif source == JobSource.URL:
            url = message.get("url") or message.get("sourceUrl")
            if not url:
                raise MessageParseError("'url' or 'sourceUrl' is required when source=URL")

        return KafkaJdPreprocessInput(
            request_id=message["requestId"],
            brand_name=message["brandName"],
            position_name=message["positionName"],
            source=source,
            text=text,
            images=images,
            url=url,
            event_id=message.get("eventId"),
            occurred_at=message.get("occurredAt"),
            version=message.get("version", "1.0"),
        )

    except MessageParseError:
        raise
    except Exception as e:
        logger.error(
            "[KAFKA_MESSAGE_PARSE_ERROR] error=%s message_keys=%s",
            str(e),
            list(message.keys()) if isinstance(message, dict) else "not_dict",
            exc_info=True,
        )
        raise MessageParseError(f"Failed to parse message: {str(e)}") from e
