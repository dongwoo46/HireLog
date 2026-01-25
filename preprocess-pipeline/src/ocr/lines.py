"""
PaddleOCR raw 결과를 line 단위 구조로 정규화하는 모듈

책임:
- OCR line 결과를 JD 파이프라인 표준 line 포맷으로 변환
- JD 도메인 기준으로 "같은 bullet 내부 줄바꿈"만 병합
"""

import re

ROW_TOLERANCE = 5          # 시각적 정렬용
MAX_Y_GAP = 25             # 같은 bullet 내 줄바꿈 허용 범위
HEIGHT_SIMILAR_RATIO = 0.8 # 스타일 동일성 보조 기준


def _sort_key(line):
    x, y, *_ = line["bbox"]
    return (y // ROW_TOLERANCE, x)


def build_lines(raw):
    """
    PaddleOCR raw 결과 → JD 파이프라인용 line 리스트 변환
    """
    lines = []

    for item in raw:
        text = item["text"].strip()
        if not text:
            continue

        score = float(item["confidence"])
        box = item["box"]
        height = item["height"]

        bbox = _calculate_bbox_from_polygon(box)

        lines.append({
            "text": text,
            "confidence_avg": score,
            "confidence_min": score,
            "low_conf_ratio": 1.0 if score < 60 else 0.0,
            "bbox": bbox,
            "token_count": len(text.split()),
            "height": height,
        })

    # 시각적 위 → 아래 정렬
    lines.sort(key=_sort_key)

    # JD 도메인 기준 줄 병합
    return merge_wrapped_lines(lines)


def _calculate_bbox_from_polygon(box):
    xs = [p[0] for p in box]
    ys = [p[1] for p in box]

    x = min(xs)
    y = min(ys)
    w = max(xs) - x
    h = max(ys) - y

    return (x, y, w, h)


# ============================================================
# JD 전용 병합 로직
# ============================================================

def merge_wrapped_lines(lines: list[dict]) -> list[dict]:
    """
    JD bullet 내부에서 줄바꿈으로 분리된 라인만 병합
    """
    if not lines:
        return []

    merged: list[dict] = []
    buffer = lines[0].copy()

    for current in lines[1:]:
        if _should_merge(buffer, current):
            _merge_into_buffer(buffer, current)
        else:
            merged.append(buffer)
            buffer = current.copy()

    merged.append(buffer)
    return merged


def _should_merge(prev: dict, curr: dict) -> bool:
    """
    병합 여부 판단 (JD 기준)
    """
    prev_text = prev["text"].strip()
    curr_text = curr["text"].strip()

    # 1. 이전 줄이 bullet이 아니면 병합 금지
    if not _is_bullet(prev_text):
        return False

    # 2. 다음 줄이 bullet이면 새로운 항목 → 병합 금지
    if _is_bullet(curr_text):
        return False

    # 3. y 간격이 너무 크면 다른 문단
    prev_y = prev["bbox"][1]
    curr_y = curr["bbox"][1]
    if curr_y - prev_y > MAX_Y_GAP:
        return False

    # 4. 글자 크기 스타일이 너무 다르면 병합 금지
    prev_h = prev.get("height", 0)
    curr_h = curr.get("height", 0)
    if prev_h and curr_h:
        ratio = min(prev_h, curr_h) / max(prev_h, curr_h)
        if ratio < HEIGHT_SIMILAR_RATIO:
            return False

    # 5. 다음 줄이 continuation 형태인지 확인
    return _is_continuation_line(curr_text)


def _merge_into_buffer(buffer: dict, current: dict):
    """
    current line을 buffer에 병합
    """
    buffer["text"] = buffer["text"].rstrip() + current["text"].lstrip()

    # bbox 확장
    x1 = min(buffer["bbox"][0], current["bbox"][0])
    y1 = min(buffer["bbox"][1], current["bbox"][1])
    x2 = max(buffer["bbox"][0] + buffer["bbox"][2],
             current["bbox"][0] + current["bbox"][2])
    y2 = max(buffer["bbox"][1] + buffer["bbox"][3],
             current["bbox"][1] + current["bbox"][3])

    buffer["bbox"] = (x1, y1, x2 - x1, y2 - y1)

    # confidence 보수적 갱신
    buffer["confidence_avg"] = (
        buffer["confidence_avg"] + current["confidence_avg"]
    ) / 2
    buffer["confidence_min"] = min(
        buffer["confidence_min"], current["confidence_min"]
    )

    buffer["token_count"] = len(buffer["text"].split())


# ============================================================
# 판단 유틸
# ============================================================

def _is_bullet(text: str) -> bool:
    """
    bullet / 번호 항목 여부
    """
    if not text:
        return False

    bullets = ('·', '-', '•', '※', '○', '●', '□', '■', '▪', '▫')
    if text.startswith(bullets):
        return True

    return bool(re.match(
        r'^(\d+[\.\)]|[가-힣][\.\)]|\(\d+\)|\([가-힣]\))\s?',
        text
    ))


def _is_continuation_line(text: str) -> bool:
    """
    bullet 내부 continuation 줄 판단
    """
    if not text:
        return False

    # JD OCR에서 자주 나오는 잘림 패턴
    prefixes = (
        '의', '를', '을', '로', '으로', '및', '서,', '며',
        '고 ', '드 ', '서 ', '와 ', '과 '
    )

    if text.startswith(prefixes):
        return True

    # 한글 조사로 시작
    return bool(re.match(r'^[가-힣]{1,2}\s', text))
