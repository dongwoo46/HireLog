from ocr.preprocess import preprocess_image
from ocr.engine import run_ocr
from ocr.lines import build_lines
from normalize.pipeline import normalize_lines
from ocr.postprocess import postprocess_jd_lines
from outputs.rawtext import build_raw_text
from ocr.confidence import classify_confidence
from ocr.quality import filter_low_quality_lines

def process_ocr_input(image_path: str):
    """
    OCR 입력 파이프라인 단일 진입점

    목적:
    - 외부 입력(image)을 JD 분석 가능한 텍스트로 변환
    - OCR 품질 상태를 함께 반환

    처리 흐름:
    1. 이미지 전처리
    2. OCR 실행
    3. 라인 구조화
    4. 문서 공통 정규화 (NFKC, 공백)
    5. normalize 파이프라인
    6. JD 후처리
    7. rawText 생성
    8. OCR 품질 상태 분류
    """

    # 1 이미지 전처리
    preprocessed_image = preprocess_image(image_path)

    # 2️ OCR 실행
    ocr_result = run_ocr(preprocessed_image)

    # 3️. OCR raw → 라인 구조화
    lines = build_lines(ocr_result["raw"])

    # 4. 라인 단위 normalize 파이프라인
    normalized = normalize_lines(lines)

    # ⭐ 4.5 라인 단위 품질 게이트 (초기 차단)
    # - confidence 낮은 라인
    # - garbage 비율 높은 라인
    # - 좌표 이상 라인 제거/격리
    filtered_lines = filter_low_quality_lines(
        normalized,
        min_confidence=45,          # 임시 기준
        max_garbage_ratio=0.6       # 임시 기준
    )

    # 5. JD 도메인 후처리
    jd_lines = postprocess_jd_lines(filtered_lines)

    # 6. 최종 rawText 생성
    raw_text = build_raw_text(jd_lines)

    # 7. OCR 품질 상태 분류
    status = classify_confidence(ocr_result["confidence"])

    return {
        "rawText": raw_text,     # 사람이 읽는 용도
        "lines": jd_lines,       # 👉 JD 파이프라인 입력용 (중요)
        "confidence": ocr_result["confidence"],
        "status": status,
    }
