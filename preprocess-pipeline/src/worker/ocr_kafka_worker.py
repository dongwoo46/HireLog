# src/worker/ocr_kafka_worker.py

"""
IMAGE(OCR) 소스 전용 Kafka Worker

책임:
- jd.preprocess.ocr.request 토픽 소비
- 메시지 파싱
- KafkaJdPreprocessOcrWorker.execute() 호출
- 결과 DTO 반환

비책임:
- Kafka 결과 발행 (BaseKafkaWorker에서 처리)
- commit / retry 판단 (BaseKafkaWorker에서 처리)
"""

import logging
from typing import Any, Dict

from infra.config.kafka_config import WorkerConfig
from infra.kafka.kafka_consumer import KafkaStreamConsumer
from infra.kafka.kafka_producer import KafkaStreamProducer
from inputs.parse_kafka_jd_preprocess import (
    parse_kafka_jd_preprocess_message,
    MessageParseError,
)
from worker.base_kafka_worker import BaseKafkaWorker
from preprocess.worker.kafka.kafka_jd_preprocess_ocr_worker import KafkaJdPreprocessOcrWorker
from outputs.kafka_jd_preprocess_output import KafkaJdPreprocessOutput
from common.exceptions import ProcessingError, ErrorCode

logger = logging.getLogger(__name__)


class OcrKafkaWorker(BaseKafkaWorker):
    """
    IMAGE(OCR) 소스 전용 Kafka Worker

    process()에서 결과 DTO를 반환한다.
    publish/commit은 BaseKafkaWorker에서 처리한다.
    """

    def __init__(
        self,
        consumer: KafkaStreamConsumer,
        producer: KafkaStreamProducer,
        result_topic: str,
        fail_topic: str,
        config: WorkerConfig,
    ):
        super().__init__(
            consumer=consumer,
            producer=producer,
            result_topic=result_topic,
            fail_topic=fail_topic,
            config=config,
            worker_name="OCR_KAFKA_WORKER",
        )
        self.jd_worker = KafkaJdPreprocessOcrWorker()

    def process(self, message: Dict[str, Any]) -> KafkaJdPreprocessOutput:
        """
        메시지 처리

        Returns:
            KafkaJdPreprocessOutput: 전처리 결과

        Raises:
            ProcessingError: 처리 실패 시
        """
        kafka_meta = message.get("_kafka_meta", {})
        offset = kafka_meta.get("offset", "unknown")

        # ==================================================
        # 1. 메시지 파싱
        # ==================================================
        try:
            jd_input = parse_kafka_jd_preprocess_message(message)
        except MessageParseError as e:
            raise ProcessingError(
                error_code=ErrorCode.MSG_PARSE_002,
                message=f"메시지 파싱 실패: {str(e)}",
                cause=e,
            )

        logger.info(
            "[OCR_KAFKA_WORKER] Processing | "
            "offset=%s requestId=%s brand=%s position=%s source=%s",
            offset,
            jd_input.request_id,
            jd_input.brand_name,
            jd_input.position_name,
            jd_input.source.value,
        )

        # ==================================================
        # 2. 전처리 실행 (결과 DTO 반환)
        # ==================================================
        return self.jd_worker.execute(jd_input)
