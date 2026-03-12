# src/infra/kafka/kafka_client_factory.py

from confluent_kafka import Producer, Consumer
import logging

from infra.config.kafka_config import KafkaConnectionConfig

logger = logging.getLogger(__name__)


class KafkaClientFactory:
    """
    Kafka Producer/Consumer 인스턴스 생성 팩토리

    책임:
    - 연결 설정(bootstrap, SASL)을 단일 지점에서 관리
    - Producer/Consumer별 운영 파라미터 적용
    - confluent-kafka 내부 에러를 Python logging으로 노출 (error_cb)
    - confluent-kafka 네이티브 인스턴스 반환
    """

    def __init__(self, connection_config: KafkaConnectionConfig) -> None:
        self._connection_config = connection_config

    def create_producer(
        self,
        client_id: str,
        compression_type: str = "snappy",
    ) -> Producer:
        def _error_cb(err):
            logger.error(
                "Kafka producer internal error",
                extra={"client_id": client_id, "error": str(err)},
            )

        config = {
            **self._connection_config.to_confluent_config(),
            "client.id": client_id,
            "acks": "all",
            "enable.idempotence": True,
            "linger.ms": 10,
            "compression.type": compression_type,
            "delivery.timeout.ms": 120000,
            "request.timeout.ms": 30000,
            "error_cb": _error_cb,
        }
        logger.info(
            "Producer created",
            extra={
                "bootstrap_servers": self._connection_config.bootstrap_servers,
                "client_id": client_id,
                "protocol": config["security.protocol"],
            },
        )
        return Producer(config)

    def create_consumer(
        self,
        group_id: str,
        client_id: str,
        auto_offset_reset: str = "earliest",
    ) -> Consumer:
        def _error_cb(err):
            logger.error(
                "Kafka consumer internal error",
                extra={"group_id": group_id, "client_id": client_id, "error": str(err)},
            )

        config = {
            **self._connection_config.to_confluent_config(),
            "group.id": group_id,
            "client.id": client_id,
            "enable.auto.commit": False,
            "auto.offset.reset": auto_offset_reset,
            "session.timeout.ms": 45000,
            "max.poll.interval.ms": 300000,
            "error_cb": _error_cb,
        }
        logger.info(
            "Consumer created",
            extra={
                "bootstrap_servers": self._connection_config.bootstrap_servers,
                "group_id": group_id,
                "client_id": client_id,
                "protocol": config["security.protocol"],
            },
        )
        return Consumer(config)
