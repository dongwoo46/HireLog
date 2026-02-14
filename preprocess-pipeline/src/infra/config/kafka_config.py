import os
from dataclasses import dataclass
from typing import Dict, Any, Optional

from dotenv import load_dotenv

load_dotenv()


@dataclass(frozen=True)
class KafkaConnectionConfig:
    """Kafka 브로커 연결 및 인증 설정 (immutable)"""
    bootstrap_servers: str
    security_protocol: str
    sasl_mechanism: str
    sasl_username: Optional[str]
    sasl_password: Optional[str]

    def validate(self) -> None:
        """SASL protocol인데 credentials 누락 시 즉시 실패"""
        if self.security_protocol in ("SASL_PLAINTEXT", "SASL_SSL"):
            missing = []
            if not self.sasl_username:
                missing.append("KAFKA_USERNAME")
            if not self.sasl_password:
                missing.append("KAFKA_PASSWORD")
            if missing:
                raise ValueError(
                    f"security.protocol={self.security_protocol} requires "
                    f"SASL credentials. Missing env vars: {', '.join(missing)}"
                )

    def to_confluent_config(self) -> Dict[str, Any]:
        """confluent-kafka 클라이언트에 전달할 연결 설정 dict 생성"""
        config: Dict[str, Any] = {
            "bootstrap.servers": self.bootstrap_servers,
            "security.protocol": self.security_protocol,
        }
        if self.security_protocol in ("SASL_PLAINTEXT", "SASL_SSL"):
            config["sasl.mechanism"] = self.sasl_mechanism
            config["sasl.username"] = self.sasl_username
            config["sasl.password"] = self.sasl_password
        return config


@dataclass(frozen=True)
class KafkaConfig:
    """Kafka 전체 설정 (연결 + 토픽 + 컨슈머 그룹)"""
    connection: KafkaConnectionConfig

    # request topics
    text_topic: str
    ocr_topic: str
    url_topic: str

    # result topics
    result_topic: str
    fail_topic: str

    consumer_group: str
    poll_timeout_sec: float
    consumer_concurrency: int

    @property
    def bootstrap_servers(self) -> str:
        return self.connection.bootstrap_servers


@dataclass(frozen=True)
class WorkerConfig:
    shutdown_timeout_sec: int
    max_retries: int = 3


def load_kafka_config() -> KafkaConfig:
    connection = KafkaConnectionConfig(
        bootstrap_servers=os.getenv("KAFKA_BOOTSTRAP_SERVERS", "127.0.0.1:19092"),
        security_protocol=os.getenv("KAFKA_SECURITY_PROTOCOL", "SASL_PLAINTEXT"),
        sasl_mechanism=os.getenv("KAFKA_SASL_MECHANISM", "PLAIN"),
        sasl_username=os.getenv("KAFKA_USERNAME"),
        sasl_password=os.getenv("KAFKA_PASSWORD"),
    )
    connection.validate()

    return KafkaConfig(
        connection=connection,
        text_topic=os.getenv("KAFKA_TEXT_TOPIC", "jd.preprocess.text.request"),
        ocr_topic=os.getenv("KAFKA_OCR_TOPIC", "jd.preprocess.ocr.request"),
        url_topic=os.getenv("KAFKA_URL_TOPIC", "jd.preprocess.url.request"),
        result_topic=os.getenv("KAFKA_RESULT_TOPIC", "jd.preprocess.response"),
        fail_topic=os.getenv("KAFKA_FAIL_TOPIC", "jd.preprocess.response.fail"),
        consumer_group=os.getenv("KAFKA_CONSUMER_GROUP", "preprocess-group"),
        poll_timeout_sec=float(os.getenv("KAFKA_POLL_TIMEOUT_SEC", "1.0")),
        consumer_concurrency=int(os.getenv("KAFKA_CONSUMER_CONCURRENCY", "3")),
    )


def load_kafka_worker_config() -> WorkerConfig:
    return WorkerConfig(
        shutdown_timeout_sec=int(os.getenv("SHUTDOWN_TIMEOUT_SEC", "30")),
        max_retries=int(os.getenv("MAX_RETRIES", "3")),
    )
