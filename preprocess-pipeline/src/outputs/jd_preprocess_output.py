# src/outputs/jd_preprocess_output.py

from dataclasses import dataclass
from typing import Any, List, Optional


@dataclass
class JdPreprocessOutput:
    """
    JD 전처리 결과 Output DTO

    책임:
    - 전처리 결과의 표준 출력 형태 정의
    - Redis Stream Result 메시지의 재료 역할
    - Worker ↔ 외부 경계 계약

    설계 원칙:
    - Spring 메시지 메타 구조와 개념적으로 대응
    - Python 내부 판단 로직은 포함하지 않음
    """

    # Message Meta (공통)
    type: str              # e.g. JD_PREPROCESS_RESULT
    message_version: str           # e.g. v1
    created_at: int                # epoch millis

    # Correlation / Context
    request_id: str
    brand_name: str
    position_name: str
    source: str                    # TEXT / IMAGE

    # Preprocess Result
    canonical_text: str

    # Optional / Debug
    sections: Optional[List[Any]] = None
