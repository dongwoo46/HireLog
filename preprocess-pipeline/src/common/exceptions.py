# src/common/exceptions.py

"""
Python Worker 단일 도메인 예외

모든 처리 실패는 ProcessingError로 래핑한다.
에러 분류는 ErrorCode(Enum)로만 표현한다.
"""

from enum import Enum
from typing import Optional


class ErrorCode(Enum):
    """
    에러 코드 분류

    네이밍 규칙: {도메인}_{순번}
    """

    # 메시지 파싱
    MSG_PARSE_001 = "MSG_PARSE_001"  # JSON 파싱 실패
    MSG_PARSE_002 = "MSG_PARSE_002"  # 필수 필드 누락

    # OCR 파이프라인
    PIPELINE_OCR_001 = "PIPELINE_OCR_001"  # OCR 추출 실패
    PIPELINE_OCR_002 = "PIPELINE_OCR_002"  # 이미지 디코딩 실패

    # TEXT 파이프라인
    PIPELINE_TEXT_001 = "PIPELINE_TEXT_001"  # 텍스트 전처리 실패

    # URL 파이프라인
    PIPELINE_URL_001 = "PIPELINE_URL_001"  # URL fetch 실패
    PIPELINE_URL_002 = "PIPELINE_URL_002"  # HTML 파싱 실패

    # 인프라
    INFRA_KAFKA_001 = "INFRA_KAFKA_001"  # Kafka produce 실패
    INFRA_STORAGE_001 = "INFRA_STORAGE_001"  # 파일 저장 실패

    # 알 수 없음
    UNKNOWN_001 = "UNKNOWN_001"  # 예상치 못한 오류


class ErrorCategory(Enum):
    """
    에러 카테고리 (Java 측 재시도 정책 힌트)
    """
    RECOVERABLE = "RECOVERABLE"  # 재시도 가능 (일시적 장애)
    PERMANENT = "PERMANENT"      # 재시도 불가 (데이터 문제)
    UNKNOWN = "UNKNOWN"          # 판단 불가


# ErrorCode → ErrorCategory 매핑
ERROR_CATEGORY_MAP = {
    # 파싱 오류: 재시도 불가
    ErrorCode.MSG_PARSE_001: ErrorCategory.PERMANENT,
    ErrorCode.MSG_PARSE_002: ErrorCategory.PERMANENT,

    # 파이프라인 오류: 재시도 불가 (데이터 문제)
    ErrorCode.PIPELINE_OCR_001: ErrorCategory.PERMANENT,
    ErrorCode.PIPELINE_OCR_002: ErrorCategory.PERMANENT,
    ErrorCode.PIPELINE_TEXT_001: ErrorCategory.PERMANENT,
    ErrorCode.PIPELINE_URL_001: ErrorCategory.RECOVERABLE,  # 네트워크 일시 장애 가능
    ErrorCode.PIPELINE_URL_002: ErrorCategory.PERMANENT,

    # 인프라 오류: 재시도 가능
    ErrorCode.INFRA_KAFKA_001: ErrorCategory.RECOVERABLE,
    ErrorCode.INFRA_STORAGE_001: ErrorCategory.RECOVERABLE,

    # 알 수 없음
    ErrorCode.UNKNOWN_001: ErrorCategory.UNKNOWN,
}


def get_error_category(error_code: ErrorCode) -> ErrorCategory:
    """ErrorCode에 대응하는 ErrorCategory 반환"""
    return ERROR_CATEGORY_MAP.get(error_code, ErrorCategory.UNKNOWN)


class ProcessingError(Exception):
    """
    Python Worker 단일 도메인 예외

    모든 처리 실패는 이 예외로 래핑한다.
    """

    def __init__(
        self,
        error_code: ErrorCode,
        message: str,
        cause: Optional[Exception] = None,
    ):
        self.error_code = error_code
        self.message = message
        self.cause = cause
        self.category = get_error_category(error_code)
        super().__init__(f"[{error_code.value}] {message}")

    def __repr__(self) -> str:
        return (
            f"ProcessingError("
            f"error_code={self.error_code.value}, "
            f"category={self.category.value}, "
            f"message={self.message!r})"
        )
