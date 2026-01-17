# ocr/header_detector.py

from statistics import median
from typing import List, Dict, Optional
import re

_DATE_PATTERN = re.compile(
    r"\d{4}[./-]\d{1,2}[./-]\d{1,2}"
)

_TIME_PATTERN = re.compile(
    r"\d{1,2}:\d{2}"
)

def compute_height_stats(lines: List[Dict]) -> Optional[Dict]:
    heights = [l["height"] for l in lines if l.get("height", 0) > 0]

    if len(heights) < 5:
        return None

    heights.sort()

    return {
        "median": median(heights),
        "p75": heights[int(len(heights) * 0.75)],
        "p90": heights[int(len(heights) * 0.9)],
        "min": heights[0],
        "max": heights[-1],
        "count": len(heights),
    }


def detect_visual_headers(lines: List[Dict]) -> List[Dict]:
    """
    line의 시각적 특징을 기반으로
    '헤더 후보(header_candidate)'를 태깅한다.
    """

    stats = compute_height_stats(lines)
    if not stats:
        for line in lines:
            line["role"] = "body"
            line["header_score"] = 0
        return lines

    base = stats["median"]

    # ⭐ 페이지 전체 폭 계산
    page_width = max(
        (l["bbox"][0] + l["bbox"][2]) for l in lines
    )

    for line in lines:
        score = _score_line_as_header(line, base, page_width)

        if score >= 3:
            line["role"] = "header_candidate"
            line["header_score"] = score
        else:
            line["role"] = "body"
            line["header_score"] = score

    return lines

def _is_right_side_ui(line: Dict, page_width: float) -> bool:
    x, _, w, _ = line.get("bbox", (0, 0, 0, 0))
    center_x = x + (w / 2)

    # 화면 오른쪽 65% 이후는 UI일 확률 매우 높음
    return center_x >= page_width * 0.65

def _looks_like_date_or_meta(text: str) -> bool:
    if _DATE_PATTERN.search(text):
        return True
    if _TIME_PATTERN.search(text):
        return True

    # 숫자 비율이 너무 높은 경우
    digits = sum(c.isdigit() for c in text)
    if digits / max(len(text), 1) >= 0.4:
        return True

    return False

def _score_line_as_header(line: Dict, base_height: float, page_width: float) -> int:
    height = line.get("height", 0)
    if height <= 0 or base_height <= 0:
        return 0

    # ❌ UI 제거
    if _is_right_side_ui(line, page_width):
        return 0

    # ❌ 날짜 / 메타 제거
    if _looks_like_date_or_meta(line.get("text", "")):
        return 0

    score = 0
    height_ratio = height / base_height

    if height_ratio >= 1.5:
        score += 3
    elif height_ratio >= 1.3:
        score += 2

    if line.get("token_count", 0) <= 6:
        score += 1

    bbox_h = line.get("bbox", (0, 0, 0, 0))[3]
    if bbox_h > 0 and (bbox_h / base_height) >= 1.3:
        score += 1

    return score
