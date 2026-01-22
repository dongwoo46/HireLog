"""
Recruitment Schedule Detector

책임:
- 상시채용 / 수시채용 / 채용시 마감 등
  '기간 없음' 상태 텍스트를 있는 그대로 수집
- 해석 / 결정 ❌
"""

from dataclasses import dataclass


@dataclass
class ScheduleMeta:
    """
    채용 일정 상태 메타데이터

    raw_texts:
        - 상시채용 / 수시채용 관련 원문 라인들
        - 복수 허용
    """
    raw_texts: list[str]


_ALWAYS_TERMS = ["상시채용", "상시"]
_OPEN_TERMS = ["수시채용", "수시", "채용시", "조기 마감"]


def extract_schedule_meta(lines: list[str]) -> ScheduleMeta | None:
    """
    JD 전체 라인에서 채용 일정 상태 메타데이터 수집

    정책:
    - 상시 / 수시 키워드가 있으면 전부 수집
    - 결정 / 우선순위 판단 ❌
    """

    raw_texts: list[str] = []

    for line in lines:
        for t in _ALWAYS_TERMS:
            if t in line:
                raw_texts.append(line)
                break

        for t in _OPEN_TERMS:
            if t in line:
                raw_texts.append(line)
                break

    if not raw_texts:
        return None

    # 중복 제거 (순서 유지)
    raw_texts = list(dict.fromkeys(raw_texts))

    return ScheduleMeta(raw_texts=raw_texts)
