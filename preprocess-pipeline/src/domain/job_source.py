# src/preprocess/domain/job_source.py

from enum import Enum


class JobSource(Enum):
    """
    채용공고 소스 타입

    Spring의 JobSourceType과 1:1 매핑
    """
    URL = "URL"
    IMAGE = "IMAGE"
    TEXT = "TEXT"

    def __str__(self) -> str:
        return self.value

    @classmethod
    def from_string(cls, value: str) -> 'JobSource':
        """
        문자열을 JobSource enum으로 변환

        Args:
            value: "URL", "IMAGE", "TEXT" 등

        Returns:
            JobSource enum

        Raises:
            ValueError: 유효하지 않은 값인 경우
        """
        try:
            return cls[value.upper()]
        except KeyError:
            valid_values = [s.name for s in cls]
            raise ValueError(
                f"Invalid JobSource: '{value}'. "
                f"Expected one of: {valid_values}"
            )