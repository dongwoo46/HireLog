# src/domain/job_platform.py

from enum import Enum


class JobPlatform(Enum):
    """
    채용공고 플랫폼 타입

    Spring의 JobPlatformType과 1:1 매핑
    """
    WANTED = "WANTED"
    REMEMBER = "REMEMBER"
    SARAMIN = "SARAMIN"
    JOBKOREA = "JOBKOREA"
    ROCKETPUNCH = "ROCKETPUNCH"
    PROGRAMMERS = "PROGRAMMERS"
    JUMPIT = "JUMPIT"
    RALLIT = "RALLIT"
    CATCH = "CATCH"
    INCRUIT = "INCRUIT"
    GREPP = "GREPP"
    LINKEDIN = "LINKEDIN"
    OTHER = "OTHER"

    def __str__(self) -> str:
        return self.value

    @classmethod
    def from_string(cls, value: str) -> "JobPlatform":
        try:
            return cls[value.upper()]
        except KeyError:
            return cls.OTHER
