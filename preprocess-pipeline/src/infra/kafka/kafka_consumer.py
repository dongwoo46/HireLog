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
        self._topic = topic
        self.poll_timeout_sec = poll_timeout_sec
        self._consumer.subscribe(
            [topic],
            on_assign=self._on_assign,
            on_revoke=self._on_revoke,
        )
        logger.info("Consumer subscribed", extra={"topic": topic})

    def _on_assign(self, consumer, partitions):
        logger.info(
            "Partition assigned",
            extra={
                "topic": self._topic,
                "partitions": [f"{p.topic}[{p.partition}]@offset={p.offset}" for p in partitions],
            },
        )

    def _on_revoke(self, consumer, partitions):
        logger.warning(
            "Partition revoked",
            extra={
                "topic": self._topic,
                "partitions": [f"{p.topic}[{p.partition}]" for p in partitions],
            },
        )

    def poll(self):
        """메시지 polling"""
        msg = self._consumer.poll(self.poll_timeout_sec)
        if msg is None:
            return None

        if msg.error():
            code = msg.error().code()
            if code == KafkaError._PARTITION_EOF:
                logger.debug("Reached end of partition", extra={"topic": self._topic})
                return None
            if code == KafkaError.UNKNOWN_TOPIC_OR_PART:
                logger.warning(
                    "Topic not yet available",
                    extra={"topic": self._topic, "error": str(msg.error())},
                )
                return None
            logger.error("Consumer poll error", extra={"topic": self._topic, "error": str(msg.error())})
            raise RuntimeError(msg.error())

        return msg

    def commit(self, msg):
        """동기 커밋"""
        try:
            self._consumer.commit(message=msg, asynchronous=False)
            logger.debug("Committed", extra={"topic": self._topic, "offset": msg.offset()})
        except Exception as e:
            raise

    def close(self):
        """Consumer 종료"""
        logger.info("Consumer closing", extra={"topic": self._topic})
        self._consumer.close()
