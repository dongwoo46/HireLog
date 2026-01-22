"""
Date / Recruitment Period Metadata

책임:
- JD 텍스트 라인에서 날짜 형태 추출
- 상시채용 / 수시채용 같은 '기간 없음' 상태 감지
- open_date / close_date / period_type 결정

원칙:
- 시간(HH:MM) 제외
- 의미 해석 최소화
- 정보 손실 ❌
"""

import re
from dataclasses import dataclass
from typing import List, Optional


# ==================================================
# Data Models
# ==================================================

@dataclass
class DateMeta:
    """
    날짜 형태 메타데이터 (순수 추출)

    raw_text:
        - 날짜가 발견된 원문 라인

    start_date / end_date:
        - 날짜 문자열 힌트
        - 범위가 아니면 end_date는 None
    """
    raw_text: str
    start_date: str
    end_date: Optional[str]


@dataclass
class RecruitmentPeriodMeta:
    """
    채용 기간 메타데이터 (최종 결과)

    period_type:
        - FIXED   : 날짜 명시
        - OPEN    : 수시채용 / 채용시 마감
        - ALWAYS  : 상시채용
        - UNKNOWN : 정보 없음

    open_date / close_date:
        - FIXED인 경우만 채워짐

    raw_texts:
        - 판단에 사용된 원문 근거
    """
    period_type: str
    open_date: Optional[str]
    close_date: Optional[str]
    raw_texts: List[str]


# ==================================================
# Regex Patterns
# ==================================================

# 시간 제거 (HH:MM)
_TIME_RE = re.compile(r"\b\d{1,2}:\d{2}\b")

# 날짜 범위
_DATE_RANGE_RE = re.compile(
    r"(\d{2,4}[./-]\d{1,2}[./-]\d{1,2})\s*~\s*"
    r"(\d{2,4}[./-]\d{1,2}[./-]\d{1,2})"
)

# 단일 날짜
_DATE_SINGLE_RE = re.compile(
    r"\b\d{4}[./-]\d{1,2}[./-]\d{1,2}\b|\b\d{1,2}[./-]\d{1,2}\b"
)

# 상태 키워드
_ALWAYS_TERMS = ["상시채용", "상시"]
_OPEN_TERMS = ["수시채용", "수시", "채용시", "조기 마감"]


# ==================================================
# Extractors
# ==================================================

def extract_date_metas(line: str) -> list[DateMeta]:
    """
    단일 라인에서 날짜 형태 전부 추출

    정책:
    - 시간(HH:MM)은 제거
    - 날짜 형태면 전부 추출
    """

    # 1️⃣ 시간 제거 (라인 자체는 유지)
    clean_line = _TIME_RE.sub("", line)

    results: list[DateMeta] = []

    # 2️⃣ 날짜 범위
    for m in _DATE_RANGE_RE.finditer(clean_line):
        results.append(
            DateMeta(
                raw_text=line,          # 원문은 그대로 보존
                start_date=m.group(1),
                end_date=m.group(2)
            )
        )

    # 3️⃣ 단일 날짜
    for m in _DATE_SINGLE_RE.finditer(clean_line):
        if any(m.group(0) in (r.start_date, r.end_date) for r in results):
            continue

        results.append(
            DateMeta(
                raw_text=line,
                start_date=m.group(0),
                end_date=None
            )
        )

    return results



def extract_recruitment_period(lines: List[str]) -> RecruitmentPeriodMeta:
    """
    JD 전체 라인을 기준으로 채용 기간 메타데이터 결정

    우선순위:
    1. 날짜 범위 존재 → FIXED
    2. 상시채용 키워드 → ALWAYS
    3. 수시/채용시 마감 → OPEN
    4. 그 외 → UNKNOWN
    """

    date_metas: List[DateMeta] = []
    raw_texts: List[str] = []

    # 1️⃣ 날짜 수집
    for line in lines:
        metas = extract_date_metas(line)
        if metas:
            date_metas.extend(metas)
            raw_texts.append(line)

    # 날짜 범위가 있으면 FIXED
    for d in date_metas:
        if d.end_date:
            return RecruitmentPeriodMeta(
                period_type="FIXED",
                open_date=d.start_date,
                close_date=d.end_date,
                raw_texts=[d.raw_text]
            )

    # 2️⃣ 상태 키워드
    for line in lines:
        if any(t in line for t in _ALWAYS_TERMS):
            return RecruitmentPeriodMeta(
                period_type="ALWAYS",
                open_date=None,
                close_date=None,
                raw_texts=[line]
            )

        if any(t in line for t in _OPEN_TERMS):
            return RecruitmentPeriodMeta(
                period_type="OPEN",
                open_date=None,
                close_date=None,
                raw_texts=[line]
            )

    # 3️⃣ 아무 정보 없음
    return RecruitmentPeriodMeta(
        period_type="UNKNOWN",
        open_date=None,
        close_date=None,
        raw_texts=[]
    )
