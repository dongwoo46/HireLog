# src/utils/logger.py

"""
Logging Configuration

사용법:
    # 개발 환경 (DEBUG 로그 출력)
    from utils.logger import setup_logging
    setup_logging(level="DEBUG")

    # 운영 환경 (INFO 로그만 출력, 기본값)
    from utils.logger import setup_logging
    setup_logging()

    # 또는 환경변수로 설정
    # LOG_LEVEL=DEBUG python main.py

디버그 로그 위치:
    - [OCR] OCR 원본 데이터 / LINE 구조화 / 전처리 완료
    - [OCR_PIPELINE] 섹션 분리 / 최종 canonical_map
    - [TEXT_PIPELINE] Core 전처리 / 섹션 분리 / 최종 canonical_map
    - [URL_PIPELINE] URL fetch / 전처리 / 섹션 분리 / 최종 canonical_map
"""

import logging
import os
import sys


def setup_logging(
    level: str | None = None,
    format_string: str | None = None,
) -> None:
    """
    전역 로깅 설정

    Args:
        level: 로그 레벨 ("DEBUG", "INFO", "WARNING", "ERROR")
               None이면 환경변수 LOG_LEVEL 사용, 없으면 INFO
        format_string: 로그 포맷 문자열
    """

    # 1. 로그 레벨 결정
    if level is None:
        level = os.environ.get("LOG_LEVEL", "INFO")

    log_level = getattr(logging, level.upper(), logging.INFO)

    # 2. 포맷 설정
    if format_string is None:
        if log_level == logging.DEBUG:
            # DEBUG 모드: 상세 정보 포함
            format_string = (
                "%(asctime)s | %(levelname)-5s | %(name)s | %(message)s"
            )
        else:
            # 운영 모드: 간결하게
            format_string = "%(asctime)s | %(levelname)-5s | %(message)s"

    # 3. 핸들러 설정
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(logging.Formatter(format_string))

    # 4. 루트 로거 설정
    root_logger = logging.getLogger()
    root_logger.setLevel(log_level)

    # 기존 핸들러 제거 (중복 방지)
    root_logger.handlers.clear()
    root_logger.addHandler(handler)

    # 5. 외부 라이브러리 로그 레벨 조정 (노이즈 감소)
    noisy_loggers = [
        "urllib3",
        "httpx",
        "httpcore",
        "asyncio",
        "PIL",
        "paddle",
        "ppocr",
    ]
    for logger_name in noisy_loggers:
        logging.getLogger(logger_name).setLevel(logging.WARNING)


def get_logger(name: str) -> logging.Logger:
    """
    모듈별 로거 생성

    Args:
        name: 로거 이름 (보통 __name__ 사용)

    Returns:
        logging.Logger
    """
    return logging.getLogger(name)
