# src/outputs/kafka_jd_preprocess_output.py

from dataclasses import dataclass, asdict
from typing import Optional, Dict, List, Any
from datetime import date
from domain.job_source import JobSource


@dataclass
class KafkaJdPreprocessOutput:
    """
    JD 전처리 결과 Kafka Event

    Spring의 JdPreprocessResponseEvent와 1:1 매핑

    용도:
    - 전처리 완료 후 Kafka로 발행할 이벤트
    - Spring 서버가 consume할 메시지 포맷

    주의:
    - Transport(Kafka) 의존 ❌
    - Domain Logic ❌
    - Validation ❌
    """

    # ===== Event Identity =====
    event_id: str          # Kafka 메시지 단위 식별자 (UUID)
    request_id: str        # 파이프라인 correlation id

    # ===== Event Metadata =====
    event_type: str        # "JD_PREPROCESS_COMPLETED"
    version: str           # "v1"
    occurred_at: int       # Unix timestamp (milliseconds)

    # ===== Context =====
    brand_name: str
    position_name: str

    # ===== Source =====
    source: JobSource
    source_url: Optional[str] = None

    # ===== Canonical Result =====
    canonical_map: Dict[str, List[str]] = None

    # ===== Recruitment Info =====
    recruitment_period_type: str = None  # "ALWAYS_OPEN", "DATE_RANGE", "UNTIL_FILLED"
    opened_date: Optional[str] = None    # ISO 8601 format (YYYY-MM-DD)
    closed_date: Optional[str] = None    # ISO 8601 format (YYYY-MM-DD)

    # ===== Extracted Skills =====
    skills: List[str] = None

    def to_dict(self) -> Dict[str, Any]:
        """
        Kafka 메시지로 직렬화

        Returns:
            JSON 직렬화 가능한 dict
        """
        data = asdict(self)

        # JobSource enum을 문자열로 변환
        if isinstance(self.source, JobSource):
            data['source'] = self.source.value

        # 필드명을 camelCase로 변환 (Spring 규약)
        kafka_message = {
            # Event Identity
            "eventId": data["event_id"],
            "requestId": data["request_id"],

            # Event Metadata
            "eventType": data["event_type"],
            "version": data["version"],
            "occurredAt": data["occurred_at"],

            # Context
            "brandName": data["brand_name"],
            "positionName": data["position_name"],

            # Source
            "source": data["source"],
            "sourceUrl": data["source_url"],

            # Canonical Result
            "canonicalMap": data["canonical_map"] or {},

            # Recruitment Info
            "recruitmentPeriodType": data["recruitment_period_type"],
            "openedDate": data["opened_date"],
            "closedDate": data["closed_date"],

            # Skills
            "skills": data["skills"] or [],
        }

        return kafka_message

    @staticmethod
    def from_domain(
            request_id: str,
            brand_name: str,
            position_name: str,
            source: JobSource,
            canonical_map: Dict[str, List[str]],
            recruitment_period_type: Optional[str] = None,
            opened_date: Optional[str] = None,
            closed_date: Optional[str] = None,
            skills: Optional[List[str]] = None,
            source_url: Optional[str] = None,
    ) -> 'KafkaJdPreprocessOutput':
        """
        도메인 데이터로부터 Kafka Event 생성

        Args:
            request_id: 요청 ID
            brand_name: 회사명
            position_name: 직무명
            source: 소스 타입
            canonical_map: 정규화된 JD 맵
            recruitment_period_type: 채용 기간 타입
            opened_date: 채용 시작일 (ISO 8601 문자열)
            closed_date: 채용 종료일 (ISO 8601 문자열)
            skills: 추출된 기술 스택
            source_url: 원본 URL (URL 소스인 경우)

        Returns:
            KafkaJdPreprocessOutput 인스턴스
        """
        import uuid
        import time

        return KafkaJdPreprocessOutput(
            # Event Identity
            event_id=str(uuid.uuid4()),
            request_id=request_id,

            # Event Metadata
            event_type="JD_PREPROCESS_COMPLETED",
            version="v1",
            occurred_at=int(time.time() * 1000),

            # Context
            brand_name=brand_name,
            position_name=position_name,

            # Source
            source=source,
            source_url=source_url,

            # Canonical Result
            canonical_map=canonical_map,

            # Recruitment Info
            recruitment_period_type=recruitment_period_type,
            opened_date=opened_date,
            closed_date=closed_date,

            # Skills
            skills=skills or [],
        )