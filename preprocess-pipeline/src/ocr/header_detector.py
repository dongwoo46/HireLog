"""
Visual Header Detection

이 모듈의 책임:
- OCR line들의 '시각적 특징'만을 기반으로
  섹션 헤더일 가능성이 있는 라인을 식별한다.

중요:
- ❌ 의미 해석 없음
- ❌ JD 도메인 판단 없음
- ✅ 오직 레이아웃 / 크기 / 위치 기반 신호 생성

이 단계의 출력은 '결정'이 아니라 '후보(signal)'이다.
"""

from statistics import median
import re
from typing import Optional


# ===============================
# 정규식: 메타/날짜 제거용
# ===============================

# 날짜 형태 (예: 2024-01-01, 2024.1.1)
_DATE_PATTERN = re.compile(r"\d{4}[./-]\d{1,2}[./-]\d{1,2}")

# 시간 형태 (예: 12:30)
_TIME_PATTERN = re.compile(r"\d{1,2}:\d{2}")


def compute_height_stats(lines: list[dict]) -> Optional[dict]:
    """
    OCR 라인들의 글자 높이(height) 통계를 계산한다.

    목적:
    - '일반 본문 대비 얼마나 큰가?'를 판단하기 위한 기준선 생성
    - 페이지별 상대 비교를 위해 절대값 대신 median 사용

    반환:
    - 통계 dict 또는 None (표본 부족 시)

    주의:
    - height 값이 있는 라인만 사용
    - 최소 5개 미만이면 신뢰 불가 → None 반환
    """

    heights = [l["height"] for l in lines if l.get("height", 0) > 0]

    if len(heights) < 5:
        return None

    heights.sort()

    return {
        "median": median(heights),                 # 기준선
        "p75": heights[int(len(heights) * 0.75)], # 참고용
        "p90": heights[int(len(heights) * 0.9)],  # 참고용
        "min": heights[0],
        "max": heights[-1],
        "count": len(heights),
    }


def detect_visual_headers(lines: list[dict]) -> list[dict]:
    """
    OCR line 리스트에 대해
    '시각적 헤더 후보(header_candidate)'를 태깅한다.

    처리 결과:
    - 각 line dict에 다음 필드가 추가됨
        - role: "header_candidate" | "body"
        - header_score: int (신호 강도)

    이 함수의 위치:
    - OCR 이후
    - 텍스트 정규화 이전 또는 이후 모두 가능
    - SectionBuilder 이전 단계

    주의:
    - 이 함수는 절대 '최종 헤더 판단'을 하지 않는다.
    """

    # 1️⃣ 글자 높이 통계 계산
    stats = compute_height_stats(lines)

    # 통계 계산 불가 → 시각적 판단 포기
    if not stats:
        for line in lines:
            line["role"] = "body"
            line["header_score"] = 0
        return lines

    base_height = stats["median"]

    # 2️⃣ 페이지 전체 폭 계산
    # bbox가 정상인 라인만 사용 (안전성 확보)
    valid_bbox_lines = [
        l for l in lines
        if (
                isinstance(l.get("bbox"), (list, tuple))
                and len(l["bbox"]) >= 3
                and isinstance(l["bbox"][0], (int, float))
                and isinstance(l["bbox"][2], (int, float))
        )
    ]

    # bbox 정보가 전혀 없으면 위치 기반 판단 불가
    if not valid_bbox_lines:
        for line in lines:
            line["role"] = "body"
            line["header_score"] = 0
        return lines

    page_width = max(
        l["bbox"][0] + l["bbox"][2]
        for l in valid_bbox_lines
    )

    # 3️⃣ 각 라인별 header score 계산
    for line in lines:
        score = _score_line_as_header(
            line=line,
            base_height=base_height,
            page_width=page_width,
        )

        line["header_score"] = score
        line["role"] = "header_candidate" if score >= 3 else "body"

    return lines


def _is_right_side_ui(line: dict, page_width: float) -> bool:
    """
    화면 오른쪽 UI 영역 여부 판단

    목적:
    - 지원 버튼, 날짜, 메타 정보 등
      본문과 무관한 UI 요소 제거
    """

    bbox = line.get("bbox")
    if not isinstance(bbox, (list, tuple)) or len(bbox) < 3:
        return False

    x, _, w = bbox[:3]
    center_x = x + (w / 2)

    # 화면 오른쪽 65% 이후는 UI일 확률이 매우 높음
    return center_x >= page_width * 0.65


def _looks_like_date_or_meta(text: str) -> bool:
    """
    날짜 / 시간 / 숫자 위주 메타 라인 판단

    목적:
    - '모집 기간', '게시일', '마감일' 등의
      헤더 오탐 제거
    """

    if _DATE_PATTERN.search(text):
        return True

    if _TIME_PATTERN.search(text):
        return True

    # 숫자 비율이 지나치게 높으면 메타로 판단
    digits = sum(c.isdigit() for c in text)
    return digits / max(len(text), 1) >= 0.4


def _score_line_as_header(
        line: dict,
        base_height: float,
        page_width: float,
) -> int:
    """
    단일 라인을 헤더로 볼 수 있는지 점수화한다.

    점수는 누적 방식이며,
    특정 조건은 즉시 탈락(return 0)시킨다.

    반환:
    - header score (정수)
    """

    height = line.get("height", 0)
    if height <= 0 or base_height <= 0:
        return 0

    # ❌ UI 영역 제거
    if _is_right_side_ui(line, page_width):
        return 0

    # ❌ 날짜 / 메타 제거
    if _looks_like_date_or_meta(line.get("text", "")):
        return 0

    score = 0
    height_ratio = height / base_height

    # 1️⃣ 글자 크기 비율
    if height_ratio >= 1.5:
        score += 3
    elif height_ratio >= 1.3:
        score += 2

    # 2️⃣ 토큰 수가 적을수록 제목일 가능성 증가
    if line.get("token_count", 0) <= 6:
        score += 1

    # 3️⃣ bbox 높이 보조 신호
    bbox = line.get("bbox")
    if isinstance(bbox, (list, tuple)) and len(bbox) >= 4:
        bbox_h = bbox[3]
        if bbox_h > 0 and (bbox_h / base_height) >= 1.3:
            score += 1

    return score
