from paddleocr import PaddleOCR
import numpy as np
from typing import Dict, Any, List


# PaddleX pipeline을 타지 않는 순수 PaddleOCR 인스턴스
# - detection + recognition만 사용
# - JD 해석과 무관한 순수 OCR 엔진 역할만 수행
_ocr = PaddleOCR(
    lang="korean",
)


def run_ocr(image: np.ndarray) -> Dict[str, Any]:
    """
    PaddleOCR 기반 OCR 엔진 실행 함수.

    역할:
    - 전처리된 이미지를 PaddleOCR에 전달
    - OCR 결과를 line 단위 raw 데이터로 정규화하여 반환

    설계 원칙:
    - JD 해석 / 의미 판단은 절대 하지 않는다
    - OCR 엔진이 제공하는 정보를 최대한 보존한다
    - PP-OCR v5 기준의 반환 구조(dict 기반)에 맞춰 처리한다
    - downstream 파이프라인이 신뢰할 수 있는 고정 포맷을 제공한다
    """

    # ----------------------------------------
    # 1️⃣ OCR 실행
    # ----------------------------------------
    # 반환 구조:
    # result = [
    #   {
    #     "rec_texts": [...],
    #     "rec_scores": [...],
    #     "rec_polys" or "rec_boxes": [...]
    #     ...
    #   }
    # ]
    result = _ocr.ocr(image)

    # OCR 결과가 비어 있거나 예상한 구조가 아닐 경우
    if not result or not isinstance(result[0], dict):
        return {
            "raw": [],
            "confidence": 0.0,
        }

    page = result[0]

    texts = page.get("rec_texts", [])
    scores = page.get("rec_scores", [])
    boxes = page.get("rec_polys") or page.get("rec_boxes") or []

    lines: List[Dict[str, Any]] = []
    confidences: List[float] = []

    # ----------------------------------------
    # 2️⃣ line 단위 결과 구성
    # ----------------------------------------
    for text, score, box in zip(texts, scores, boxes):

        # 텍스트 기본 검증
        if not text:
            continue

        text = text.strip()
        if not text:
            continue

        # confidence 값 검증
        try:
            score = float(score)
        except (TypeError, ValueError):
            continue

        # ------------------------------------
        # 3️⃣ 글자 크기(height) 추정
        # ------------------------------------
        # box는 보통 (N, 2) 형태의 polygon 또는 bounding box
        height = None
        try:
            if hasattr(box, "ndim") and box.ndim == 2 and box.shape[1] == 2:
                ys = box[:, 1]
                height = float(max(ys) - min(ys))
        except Exception:
            height = None

        # ------------------------------------
        # 4️⃣ raw line 저장
        # ------------------------------------
        lines.append({
            "text": text,
            "confidence": score * 100.0,   # 0~100 스케일로 통일
            "box": box,                    # 원본 좌표 (후속 판단용)
            "height": height               # 글자 크기 추정값
        })

        confidences.append(score)

    # ----------------------------------------
    # 5️⃣ 전체 OCR 품질 지표 계산
    # ----------------------------------------
    avg_confidence = (
        sum(confidences) / len(confidences)
        if confidences else 0.0
    )

    # OCR 단계의 출력은
    # "의미 해석 이전의 원재료(raw material)"만 제공하는 것이 목적
    return {
        "raw": lines,                         # line-level raw 데이터
        "confidence": avg_confidence * 100.0 # 전체 OCR 품질 지표
    }
