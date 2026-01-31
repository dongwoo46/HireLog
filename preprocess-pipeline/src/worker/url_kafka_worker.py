# src/worker/url_kafka_worker.py

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
from preprocess.worker.kafka.kafka_jd_preprocess_url_worker import (
    KafkaJdPreprocessUrlWorker,
)

logger = logging.getLogger(__name__)


class UrlKafkaWorker(BaseKafkaWorker):
    """
    URL 소스 전용 Kafka Worker

    책임:
    - jd.preprocess.url.request 토픽 소비
    - 메시지 파싱
    - KafkaJdPreprocessUrlWorker 호출
    - commit / retry 판단

    비책임:
    - 전처리 로직
    - Kafka 결과 발행
    """

    def __init__(
            self,
            consumer: KafkaStreamConsumer,
            producer: KafkaStreamProducer,
            result_topic: str,
            config: WorkerConfig,
    ):
        super().__init__(
            consumer=consumer,
            config=config,
            worker_name="URL_KAFKA_WORKER",
        )
        self.jd_worker = KafkaJdPreprocessUrlWorker(
            kafka_producer=producer,
            result_topic=result_topic,
        )

    def process(self, message: Dict[str, Any]) -> bool:
        """
        Returns:
            True  -> 처리 + Kafka publish까지 완료 (commit)
            False -> 처리 실패 (재처리)
        """
        kafka_meta = message.get("_kafka_meta", {})
        offset = kafka_meta.get("offset", "unknown")

        try:
            # ==================================================
            # 1️⃣ 메시지 파싱
            # ==================================================
            jd_input = parse_kafka_jd_preprocess_message(message)

            logger.info(
                "[URL_KAFKA_WORKER] Processing | "
                "offset=%s requestId=%s brand=%s url=%s source=%s",
                offset,
                jd_input.request_id,
                jd_input.brand_name,
                jd_input.url,
                jd_input.source.value,
            )

            # ==================================================
            # 2️⃣ 전처리 + Kafka 결과 발행
            # ==================================================
            self.jd_worker.process(jd_input)

            logger.info(
                "[URL_KAFKA_WORKER] Success | offset=%s requestId=%s",
                offset,
                jd_input.request_id,
            )

            return True

        except MessageParseError as e:
            # 파싱 오류: 재시도해도 실패 → commit
            logger.error(
                "[URL_KAFKA_WORKER] Message parse error | offset=%s error=%s",
                offset,
                str(e),
            )
            return True

        except Exception as e:
            # 처리 실패: commit ❌ → Kafka 재처리
            logger.error(
                "[URL_KAFKA_WORKER] Processing failed | offset=%s error=%s",
                offset,
                str(e),
                exc_info=True,
            )
            return False
