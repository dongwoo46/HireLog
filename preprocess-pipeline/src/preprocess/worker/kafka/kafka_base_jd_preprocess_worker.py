# src/preprocess/worker/kafka/kafka_base_jd_preprocess_worker.py

"""
Kafka 기반 JD 전처리 Worker 공통 베이스

책임:
- Output DTO 생성
- 날짜 포맷 정규화

비책임:
- Kafka publish (BaseKafkaWorker에서 담당)
- commit / retry 판단 (BaseKafkaWorker에서 담당)
"""

import logging
from abc import ABC, abstractmethod
from datetime import datetime

from outputs.kafka_jd_preprocess_output import KafkaJdPreprocessOutput

logger = logging.getLogger(__name__)


class KafkaBaseJdPreprocessWorker(ABC):
    """
    Kafka 기반 JD 전처리 Worker 공통 베이스

    하위 Worker는 process()에서 결과 DTO만 반환한다.
    Kafka 발행은 상위 BaseKafkaWorker에서 일괄 처리한다.
    """

    def __init__(self):
        pass

    # ==================================================
    # Internal Utils
    # ==================================================

    def _normalize_date(self, date_str: str) -> str | None:
        """날짜 문자열 ISO 8601 정규화"""
        if not date_str:
            return None

        for fmt in ("%Y.%m.%d", "%Y-%m-%d", "%Y/%m/%d"):
            try:
                return datetime.strptime(date_str, fmt).date().isoformat()
            except ValueError:
                continue

        logger.warning("[DATE_NORMALIZE_FAILED] date_str=%s", date_str)
        return None

    # ==================================================
    # Abstract
    # ==================================================

    @abstractmethod
    def execute(self, input) -> KafkaJdPreprocessOutput:
        """
        전처리 실행 (결과 DTO 반환)

        규칙:
        - 성공 시 KafkaJdPreprocessOutput 반환
        - 실패 시 ProcessingError 발생
        - Kafka 발행하지 않음 (상위에서 처리)
        """
        raise NotImplementedError
