# src/utils/logger.py

"""
Logging Configuration (Grafana Loki / Promtail 호환 JSON 포맷)

출력 형식 (1줄 JSON):
    {"timestamp":"2026-02-23T02:29:01.318Z","level":"INFO","logger":"worker.base_kafka_worker",
     "pid":1234,"message":"Worker started","worker_name":"OCR_KAFKA_WORKER"}

Promtail pipeline 라벨 추출 권장 필드:
    - level       : 로그 레벨 (INFO / WARNING / ERROR / DEBUG)
    - logger      : 모듈명
    - worker_name : 워커 식별자 (OCR_KAFKA_WORKER, TEXT_KAFKA_WORKER, URL_KAFKA_WORKER)

환경변수:
    LOG_LEVEL=DEBUG  python main_kafka.py   # 개발
    LOG_LEVEL=INFO   python main_kafka.py   # 운영 (기본값)
"""

import logging
import os
import sys
from datetime import datetime, timezone

from pythonjsonlogger import jsonlogger


class LokiJsonFormatter(jsonlogger.JsonFormatter):
    """Grafana Loki / Promtail 호환 JSON 포맷터

    표준 필드 : timestamp, level, logger, pid, message
    추가 필드 : extra={}로 전달된 키-값 쌍 (worker_name, offset, request_id 등)
    """

    def add_fields(self, log_record, record, message_dict):
        super().add_fields(log_record, record, message_dict)

        dt = datetime.fromtimestamp(record.created, tz=timezone.utc)
        log_record["timestamp"] = (
            dt.strftime("%Y-%m-%dT%H:%M:%S.") + f"{int(dt.microsecond / 1000):03d}Z"
        )
        log_record["level"] = record.levelname
        log_record["logger"] = record.name
        log_record["pid"] = record.process

        for key in ("levelname", "name", "asctime"):
            log_record.pop(key, None)


def setup_logging(level: str | None = None) -> None:
    """전역 로깅 설정

    Args:
        level: 로그 레벨 ("DEBUG", "INFO", "WARNING", "ERROR")
               None이면 환경변수 LOG_LEVEL 사용, 없으면 INFO
    """
    if level is None:
        level = os.environ.get("LOG_LEVEL", "INFO")

    log_level = getattr(logging, level.upper(), logging.INFO)

    formatter = LokiJsonFormatter(json_ensure_ascii=False)
    handler = logging.StreamHandler(sys.stderr)
    handler.setFormatter(formatter)

    root_logger = logging.getLogger()
    root_logger.setLevel(log_level)
    root_logger.handlers.clear()
    root_logger.addHandler(handler)

    noisy_loggers = ["urllib3", "httpx", "httpcore", "asyncio", "PIL", "paddle", "ppocr"]
    for logger_name in noisy_loggers:
        logging.getLogger(logger_name).setLevel(logging.WARNING)


def get_logger(name: str) -> logging.Logger:
    return logging.getLogger(name)
