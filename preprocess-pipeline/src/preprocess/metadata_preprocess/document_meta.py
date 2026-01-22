"""
Document-level Metadata Container

책임:
- 여러 메타데이터를 하나로 묶는 컨테이너
- 파이프라인 전체에서 전달되는 단일 객체
"""

from dataclasses import dataclass
from .date_meta import RecruitmentPeriodMeta


@dataclass
class DocumentMeta:
    recruitment_period: RecruitmentPeriodMeta


