from confluent_kafka import Consumer, KafkaError
import logging

logger = logging.getLogger(__name__)


class KafkaStreamConsumer:
    """confluent-kafka 기반 Consumer"""

    def __init__(
            self,
            bootstrap_servers: str,
            topic: str,
            group_id: str,
            client_id: str,
            poll_timeout_sec: float = 1.0,
            auto_offset_reset: str = "earliest",  # 설정 가능하게
    ):
        config = {
            "bootstrap.servers": bootstrap_servers,
            "group.id": group_id,
            "client.id": client_id,
            "enable.auto.commit": False,
            "auto.offset.reset": auto_offset_reset,
            # 추가 권장 설정
            "session.timeout.ms": 45000,
            "max.poll.interval.ms": 300000,
        }

        self.consumer = Consumer(config)
        self.consumer.subscribe([topic])
        self.poll_timeout_sec = poll_timeout_sec
        logger.info(f"Consumer initialized - topic: {topic}, group: {group_id}")

    def poll(self):
        """메시지 polling"""
        msg = self.consumer.poll(self.poll_timeout_sec)
        if msg is None:
            return None

        if msg.error():
            if msg.error().code() == KafkaError._PARTITION_EOF:
                logger.debug("Reached end of partition")
                return None
            logger.error(f"Consumer error: {msg.error()}")
            raise RuntimeError(msg.error())

        return msg

    def commit(self, msg):
        """동기 커밋"""
        try:
            self.consumer.commit(message=msg, asynchronous=False)
            logger.debug(f"Committed offset: {msg.offset()}")
        except Exception as e:
            logger.error(f"Commit failed: {e}")
            raise

    def close(self):
        """Consumer 종료"""
        logger.info("Closing consumer")
        self.consumer.close()