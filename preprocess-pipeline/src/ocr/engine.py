from paddleocr import PaddleOCR
import numpy as np
from typing import TypedDict


_ocr = PaddleOCR(
    lang="korean",
    use_angle_cls=True,
)

class OCRLine(TypedDict):
    text: str
    confidence: float
    box: np.ndarray | list | None
    height: float | None

class OCRResult(TypedDict):
    raw: list[OCRLine]
    confidence: float

def run_ocr(image: np.ndarray) -> OCRResult:
    result = _ocr.ocr(image)

    # 2.x 반환 구조:
    # result = [ [[[x1,y1],[x2,y2],[x3,y3],[x4,y4]], ("text", score)], ... ] ]
    if not result or not result[0]:
        return {
            "raw": [],
            "confidence": 0.0,
        }

    lines: list[OCRLine] = []
    confidences: list[float] = []

    for line in result[0]:
        if not line or len(line) < 2:
            continue

        box = line[0]
        text_info = line[1]

        if not text_info or len(text_info) < 2:
            continue

        text = text_info[0]
        score = text_info[1]

        if not text:
            continue

        text = text.strip()
        if not text:
            continue

        try:
            score = float(score)
        except (TypeError, ValueError):
            continue

        height = None
        try:
            box_arr = np.array(box)
            if box_arr.ndim == 2 and box_arr.shape[1] == 2:
                ys = box_arr[:, 1]
                height = float(max(ys) - min(ys))
        except Exception:
            height = None

        lines.append({
            "text": text,
            "confidence": score * 100.0,
            "box": box,
            "height": height
        })

        confidences.append(score)

    avg_confidence = (
        sum(confidences) / len(confidences)
        if confidences else 0.0
    )

    return {
        "raw": lines,
        "confidence": avg_confidence * 100.0
    }
