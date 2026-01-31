# src/inputs/kafka_jd_preprocess_input.py

from dataclasses import dataclass
from typing import Optional
from domain.job_source import JobSource


@dataclass
class KafkaJdPreprocessInput:
    """
    Kafka JD 전처리 입력 모델

    Spring의 JdPreprocessRequestMessage와 1:1 매핑
    """

    # 필수 필드
    request_id: str
    brand_name: str
    position_name: str
    source: JobSource
    text: str

    # 선택적 메타데이터 (Kafka 메시지에서 추가)
    event_id: Optional[str] = None
    occurred_at: Optional[int] = None
    version: Optional[str] = None