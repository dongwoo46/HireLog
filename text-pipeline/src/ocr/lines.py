"""
PaddleOCR raw 결과를 line 단위 구조로 정규화하는 모듈

용도:
- PaddleOCR의 line-level 결과를
  JD 파이프라인에서 공통으로 사용하는 line 포맷으로 변환
- 이후 normalize / quality filter / JD 후처리의 기준 입력 생성
"""


def build_lines(raw):
    """
    PaddleOCR raw 결과를 line 단위 구조로 변환한다.

    입력:
    - raw: run_ocr()에서 반환된 PaddleOCR raw 리스트
      [
        {
          text: str,
          confidence: float,   # 0~100
          box: [[x,y], ...],   # 4-point polygon
          height: float        # 글자 크기 추정값
        }
      ]

    출력:
    - [
        {
          text: str,                  # 한 줄의 텍스트
          confidence_avg: float,      # 줄 confidence (PaddleOCR score)
          confidence_min: float,      # 동일 line이므로 avg와 동일
          low_conf_ratio: float,      # 동일 line이므로 0 또는 1
          bbox: (x, y, w, h),         # line bounding box
          token_count: int,           # 공백 기준 토큰 수
          height: float               # 글자 크기 추정값
        }
      ]
    """

    lines = []

    for item in raw:
        text = item["text"].strip()
        if not text:
            continue

        score = float(item["confidence"])
        box = item["box"]
        height = item["height"]

        # PaddleOCR는 이미 line 단위 결과이므로
        # confidence 통계는 단순화한다.
        confidence_avg = score
        confidence_min = score
        low_conf_ratio = 1.0 if score < 60 else 0.0

        # polygon box를 axis-aligned bbox로 변환
        bbox = _calculate_bbox_from_polygon(box)

        lines.append({
            "text": text,
            "confidence_avg": confidence_avg,
            "confidence_min": confidence_min,
            "low_conf_ratio": low_conf_ratio,
            "bbox": bbox,
            "token_count": len(text.split()),
            "height": height
        })

    # 시각적 위 → 아래 순서를 보장하기 위해 y 좌표 기준 정렬
    lines.sort(key=lambda x: x["bbox"][1])

    return lines


def _calculate_bbox_from_polygon(box):
    """
    PaddleOCR polygon bounding box를
    axis-aligned bounding box로 변환한다.

    입력:
    - box: [[x1,y1], [x2,y2], [x3,y3], [x4,y4]]

    출력:
    - (x, y, w, h)
    """

    xs = [p[0] for p in box]
    ys = [p[1] for p in box]

    x = min(xs)
    y = min(ys)
    w = max(xs) - x
    h = max(ys) - y

    return (x, y, w, h)
