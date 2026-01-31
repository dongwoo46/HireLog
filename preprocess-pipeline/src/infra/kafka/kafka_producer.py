# src/infra/kafka/kafka_producer.py

from confluent_kafka import Producer
import logging
import json
from typing import Optional, Dict, Any

logger = logging.getLogger(__name__)


class KafkaStreamProducer:
    """
    confluent-kafka 기반 Producer 래퍼

    책임:
    - Kafka 메시지 발행
    - 직렬화(JSON)
    - delivery callback 로깅

    계약:
    - produce enqueue 실패 시 예외 발생
    - delivery 실패는 callback 로깅만 수행
    - 중복/멱등성 판단은 downstream consumer 책임

    """

    def __init__(
            self,
            bootstrap_servers: str,
            client_id: str,
            compression_type: str = "snappy",
    ):
        # ✅ Producer 내부 설정 (외부로 노출 ❌)
        config = {
            "bootstrap.servers": bootstrap_servers,
            "client.id": client_id,

            # 신뢰성
            "acks": "all",
            "enable.idempotence": True,

            # 성능
            "linger.ms": 10,
            "compression.type": compression_type,

            # 전송 안정성
            "delivery.timeout.ms": 120000,
            "request.timeout.ms": 30000,
        }

        self._producer = Producer(config)
        logger.info(
            "[KAFKA_PRODUCER_INIT] servers=%s client_id=%s",
            bootstrap_servers,
            client_id,
        )

    def publish(
            self,
            topic: str,
            message: Any,
            key: Optional[str] = None,
            headers: Optional[Dict[str, str]] = None,
    ) -> None:
        """
        메시지 발행 (비동기)

        실패 시 예외 발생 → 상위 Consumer에서 재처리 판단
        """
        if isinstance(message, dict):
            message = json.dumps(message, ensure_ascii=False).encode("utf-8")
        elif isinstance(message, str):
            message = message.encode("utf-8")

        if key and isinstance(key, str):
            key = key.encode("utf-8")

        kafka_headers = None
        if headers:
            kafka_headers = [(k, v.encode("utf-8")) for k, v in headers.items()]

        self._producer.produce(
            topic=topic,
            value=message,
            key=key,
            headers=kafka_headers,
            on_delivery=self._delivery_callback,
        )

        # 이벤트 루프 태우기 (non-blocking)
        self._producer.poll(0)

    def flush(self, timeout: float = 10.0):
        remaining = self._producer.flush(timeout)
        if remaining > 0:
            logger.warning(
                "[KAFKA_PRODUCER_FLUSH] %d messages not delivered",
                remaining,
            )

    def close(self):
        logger.info("[KAFKA_PRODUCER_CLOSE]")
        self.flush()

    @staticmethod
    def _delivery_callback(err, msg):
        if err:
            logger.error(
                "[KAFKA_DELIVERY_FAILED] topic=%s partition=%s error=%s",
                msg.topic(),
                msg.partition(),
                err,
            )
        else:
            logger.debug(
                "[KAFKA_DELIVERED] topic=%s partition=%s offset=%s",
                msg.topic(),
                msg.partition(),
                msg.offset(),
            )
