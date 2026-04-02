# src/domain/job_platform.py

from enum import Enum
from urllib.parse import urlparse


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
    def from_string(cls, value: str | None) -> "JobPlatform":
        if isinstance(value, cls):
            return value
        if value is None:
            return cls.OTHER
        if not isinstance(value, str):
            value = str(value)
        value = value.strip()
        if not value:
            return cls.OTHER
        try:
            return cls[value.upper()]
        except KeyError:
            return cls.OTHER

    @classmethod
    def from_url(cls, url: str | None) -> "JobPlatform":
        if not url:
            return cls.OTHER

        try:
            hostname = (urlparse(url).hostname or "").lower()
        except Exception:
            return cls.OTHER

        domain_map: list[tuple[str, "JobPlatform"]] = [
            ("wanted.co.kr", cls.WANTED),
            ("rememberapp.co.kr", cls.REMEMBER),
            ("saramin.co.kr", cls.SARAMIN),
            ("jobkorea.co.kr", cls.JOBKOREA),
            ("rocketpunch.com", cls.ROCKETPUNCH),
            ("programmers.co.kr", cls.PROGRAMMERS),
            ("jumpit.saramin.co.kr", cls.JUMPIT),
            ("jumpit.co.kr", cls.JUMPIT),
            ("rallit.com", cls.RALLIT),
            ("catch.co.kr", cls.CATCH),
            ("incruit.com", cls.INCRUIT),
            ("grepp.co", cls.GREPP),
            ("linkedin.com", cls.LINKEDIN),
        ]

        for domain, platform in domain_map:
            if hostname == domain or hostname.endswith(f".{domain}"):
                return platform

        return cls.OTHER
