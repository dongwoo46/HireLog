from confluent_kafka import Consumer, KafkaError
import logging

logger = logging.getLogger(__name__)


class KafkaStreamConsumer:
    """
    confluent-kafka 기반 Consumer 래퍼

    책임:
    - 메시지 polling 및 에러 핸들링
    - 수동 offset commit
    - 연결/인증 설정은 KafkaClientFactory가 담당
    """

    def __init__(
            self,
            consumer: Consumer,
            topic: str,
            poll_timeout_sec: float = 1.0,
    ):
        self._consumer = consumer
        self.poll_timeout_sec = poll_timeout_sec
        self._consumer.subscribe([topic])
        logger.info("Consumer subscribed - topic: %s", topic)

    def poll(self):
        """메시지 polling"""
        msg = self._consumer.poll(self.poll_timeout_sec)
        if msg is None:
            return None

        if msg.error():
            if msg.error().code() == KafkaError._PARTITION_EOF:
                logger.debug("Reached end of partition")
                return None
            logger.error("Consumer error: %s", msg.error())
            raise RuntimeError(msg.error())

        return msg

    def commit(self, msg):
        """동기 커밋"""
        try:
            self._consumer.commit(message=msg, asynchronous=False)
            logger.debug("Committed offset: %s", msg.offset())
        except Exception as e:
            logger.error("Commit failed: %s", e)
            raise

    def close(self):
        """Consumer 종료"""
        logger.info("Closing consumer")
        self._consumer.close()
