# src/preprocess/worker/base_preprocess_worker.py

import logging
import json
from datetime import datetime

from outputs.jd_preprocess_output import JdPreprocessOutput
from infra.redis.stream_publisher import RedisStreamPublisher
from infra.redis.redis_client import RedisClient
from infra.redis.stream_serializer import RedisStreamSerializer

logger = logging.getLogger(__name__)


class BaseJdPreprocessWorker:
    """
    JD 전처리 Worker 공통 베이스

    책임:
    - Output DTO → Redis Stream 메시지 직렬화
    - 메시지 계약(metadata / payload) 일관성 보장
    - 날짜 / JSON 포맷 정규화
    - Stream publish

    비책임:
    - 전처리 로직(TEXT / OCR / URL) ❌
    - canonical 데이터 생성 ❌
    - Stream 선택 정책 ❌
    """

    def __init__(self):
        redis_client = RedisClient()
        self.publisher = RedisStreamPublisher(redis_client)

    # ==================================================
    # Internal Utils
    # ==================================================

    def _normalize_date(self, date_str: str) -> str | None:
        """
        날짜 문자열 정규화

        Python → Spring 계약:
        - yyyy.MM.dd, yyyy-MM-dd, yyyy/MM/dd → yyyy-MM-dd (ISO-8601)

        지원 포맷:
        - 2024.01.15
        - 2024-01-15
        - 2024/01/15

        Returns:
            정규화된 날짜 문자열 또는 파싱 실패 시 None
        """
        if not date_str:
            return None

        date_formats = [
            "%Y.%m.%d",
            "%Y-%m-%d",
            "%Y/%m/%d",
        ]

        for fmt in date_formats:
            try:
                return datetime.strptime(date_str, fmt).date().isoformat()
            except ValueError:
                continue

        logger.warning("Date normalization failed", extra={"date_str": date_str})
        return None

    # ==================================================
    # Publish
    # ==================================================

    def _publish_result(
        self,
        *,
        output: JdPreprocessOutput,
        stream_key: str,
    ) -> str:
        """
        전처리 결과 publish

        흐름:
        1. Output DTO → payload / metadata 분리
        2. 계약된 Redis Stream 메시지로 직렬화
        3. Stream publish
        """

        # ==================================================
        # Payload (🔥 비즈니스 데이터)
        # - 모든 key는 payload.* 규칙을 따른다
        # ==================================================
        payload: dict[str, str] = {
            "canonicalMap": json.dumps(
                output.canonical_map,
                ensure_ascii=False
            ),
            "source": output.source,
        }

        # ==================================================
        # Recruitment Period (존재하는 경우만 포함)
        # ==================================================
        if output.recruitment_period_type is not None:
            payload["recruitmentPeriodType"] = output.recruitment_period_type

        if output.recruitment_open_date is not None:
            normalized_open = self._normalize_date(output.recruitment_open_date)
            if normalized_open:
                payload["recruitmentOpenDate"] = normalized_open

        if output.recruitment_close_date is not None:
            normalized_close = self._normalize_date(output.recruitment_close_date)
            if normalized_close:
                payload["recruitmentCloseDate"] = normalized_close

        # ==================================================
        # Skills (존재하는 경우만 포함)
        # ==================================================
        if output.skills is not None:
            payload["skills"] = json.dumps(output.skills, ensure_ascii=False)

        # ==================================================
        # Metadata (🔥 메시지 식별 / 추적)
        # ==================================================
        metadata = {
            "type": output.type,
            "requestId": output.request_id,
            "brandName": output.brand_name,
            "positionName": output.position_name,
            "createdAt": str(output.created_at),
            "messageVersion": output.message_version,
        }

        # ==================================================
        # Stream Message 직렬화
        # ==================================================
        message = RedisStreamSerializer.serialize(
            metadata=metadata,
            payload=payload,
        )

        # ==================================================
        # Publish
        # ==================================================
        entry_id = self.publisher.publish(
            stream_key=stream_key,
            message=message,
        )

        logger.info(
            "JD preprocess result published",
            extra={
                "request_id": output.request_id,
                "brand_name": output.brand_name,
                "position_name": output.position_name,
                "source": output.source,
                "stream_key": stream_key,
                "entry_id": entry_id,
            },
        )

        return entry_id
