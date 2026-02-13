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
    - confluent-kafka 네이티브 인스턴스 반환

    연결 설정 변경(SASL_SSL 전환 등)은 KafkaConnectionConfig만 수정하면 됨
    """

    def __init__(self, connection_config: KafkaConnectionConfig) -> None:
        self._connection_config = connection_config

    def create_producer(
        self,
        client_id: str,
        compression_type: str = "snappy",
    ) -> Producer:
        config = {
            **self._connection_config.to_confluent_config(),
            "client.id": client_id,
            "acks": "all",
            "enable.idempotence": True,
            "linger.ms": 10,
            "compression.type": compression_type,
            "delivery.timeout.ms": 120000,
            "request.timeout.ms": 30000,
        }
        logger.info(
            "[KAFKA_FACTORY] Producer created - servers=%s client_id=%s protocol=%s",
            self._connection_config.bootstrap_servers,
            client_id,
            config["security.protocol"],
        )
        return Producer(config)

    def create_consumer(
        self,
        group_id: str,
        client_id: str,
        auto_offset_reset: str = "earliest",
    ) -> Consumer:
        config = {
            **self._connection_config.to_confluent_config(),
            "group.id": group_id,
            "client.id": client_id,
            "enable.auto.commit": False,
            "auto.offset.reset": auto_offset_reset,
            "session.timeout.ms": 45000,
            "max.poll.interval.ms": 300000,
        }
        logger.info(
            "[KAFKA_FACTORY] Consumer created - servers=%s group=%s client_id=%s protocol=%s",
            self._connection_config.bootstrap_servers,
            group_id,
            client_id,
            config["security.protocol"],
        )
        return Consumer(config)
