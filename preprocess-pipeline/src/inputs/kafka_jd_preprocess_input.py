# src/inputs/kafka_jd_preprocess_input.py

from dataclasses import dataclass, field
from typing import Optional, List
from domain.job_source import JobSource


@dataclass
class KafkaJdPreprocessInput:
    """
    Kafka JD 전처리 입력 모델

    Spring의 JdPreprocessRequestMessage와 1:1 매핑

    계약:
    - TEXT 소스: text 필드 필수
    - IMAGE 소스: images 필드 필수
    - URL 소스: url 필드 필수
    """

    # 필수 필드
    request_id: str
    brand_name: str
    position_name: str
    source: JobSource

    # 소스별 필수 필드 (source에 따라 1개만 사용)
    text: Optional[str] = None
    images: Optional[List[str]] = field(default_factory=list)
    url: Optional[str] = None

    # 선택적 메타데이터 (Kafka 메시지에서 추가)
    event_id: Optional[str] = None
    occurred_at: Optional[int] = None
    version: Optional[str] = None