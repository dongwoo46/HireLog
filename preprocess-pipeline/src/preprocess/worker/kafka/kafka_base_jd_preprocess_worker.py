# src/preprocess/worker/kafka/base_kafka_jd_preprocess_worker.py

import logging
from abc import ABC, abstractmethod
from datetime import datetime

from outputs.kafka_jd_preprocess_output import KafkaJdPreprocessOutput
from infra.kafka.kafka_producer import KafkaStreamProducer

logger = logging.getLogger(__name__)


class KafkaBaseJdPreprocessWorker(ABC):
    """
    Kafka 기반 JD 전처리 Worker 공통 베이스

    책임:
    - Output DTO → Kafka 메시지 직렬화
    - JSON schema 계약 일관성 보장
    - 날짜 포맷 정규화
    - Kafka publish (비동기)

    비책임:
    - 전처리 로직(TEXT / OCR / URL)
    - commit / retry 판단
    - Topic 선택 정책 (외부 주입)
    """

    def __init__(self, producer: KafkaStreamProducer, result_topic: str):
        # 모든 JD 전처리 결과는 단일 response topic으로 발행한다
        self.producer = producer
        self.result_topic = result_topic

    # ==================================================
    # Internal Utils
    # ==================================================

    def _normalize_date(self, date_str: str) -> str | None:
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
    def process(self, input) -> KafkaJdPreprocessOutput:
        """
        전처리 실행

        규칙:
        - Kafka publish 실패 시 반드시 예외 발생
        - commit / retry 판단은 Consumer 책임
        """
        raise NotImplementedError

    # ==================================================
    # Publish
    # ==================================================

    def _publish_result(self, output: KafkaJdPreprocessOutput) -> None:
        """
        Kafka 결과 발행

        계약:
        - produce 실패 시 예외 발생 (호출자가 재처리 판단)
        - 예외 없이 완료 = 성공
        - 반환값 없음 (void)
        """
        message = output.to_dict()
        key = output.request_id

        logger.info(
            "[JD_PREPROCESS_PUBLISH] topic=%s requestId=%s brand=%s position=%s source=%s",
            self.result_topic,
            output.request_id,
            output.brand_name,
            output.position_name,
            output.source.value if hasattr(output.source, 'value') else output.source,
        )

        try:
            self.producer.publish(
                topic=self.result_topic,
                message=message,
                key=key,
            )
        except Exception as e:
            logger.error(
                "[JD_PREPROCESS_PUBLISH_FAILED] requestId=%s error=%s",
                output.request_id,
                str(e),
                exc_info=True,
            )
            raise
