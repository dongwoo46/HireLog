
import os
from dataclasses import dataclass


@dataclass(frozen=True)
class KafkaConfig:
    bootstrap_servers: str

    # request topics
    text_topic: str
    ocr_topic: str
    url_topic: str

    # result topic (하나)
    result_topic: str

    consumer_group: str
    poll_timeout_sec: float



@dataclass
class WorkerConfig:
    """Worker 설정"""
    shutdown_timeout_sec: int
    max_retries: int = 3


def load_kafka_config() -> KafkaConfig:
    """Kafka 설정 로드"""
    return KafkaConfig(
        bootstrap_servers=os.getenv("KAFKA_BOOTSTRAP_SERVERS", "127.0.0.1:19092"),
        text_topic=os.getenv("KAFKA_TEXT_TOPIC", "jd.preprocess.text.request"),
        ocr_topic=os.getenv("KAFKA_OCR_TOPIC", "jd.preprocess.ocr.request"),
        url_topic=os.getenv("KAFKA_URL_TOPIC", "jd.preprocess.url.request"),
        result_topic=os.getenv("KAFKA_RESULT_TOPIC", "jd.preprocess.response"),
        consumer_group=os.getenv("KAFKA_CONSUMER_GROUP", "preprocess-group"),
        poll_timeout_sec=float(os.getenv("KAFKA_POLL_TIMEOUT_SEC", "1.0")),
    )


def load_kafka_worker_config() -> WorkerConfig:
    """Worker 설정 로드"""
    return WorkerConfig(
        shutdown_timeout_sec=int(os.getenv("SHUTDOWN_TIMEOUT_SEC", "30")),
        max_retries=int(os.getenv("MAX_RETRIES", "3")),
    )