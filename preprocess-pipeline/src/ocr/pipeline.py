import os
import logging
from ocr.preprocess import preprocess_image
from ocr.engine import run_ocr
from ocr.lines import build_lines
from normalize.pipeline import normalize_lines
from ocr.postprocess import postprocess_ocr_lines
from utils.rawtext import build_raw_text
from ocr.confidence import classify_confidence
from ocr.quality import filter_low_quality_lines
from ocr.header_detector import detect_visual_headers

logger = logging.getLogger(__name__)

def process_ocr_input(image_input: str | list[str]):
    """
    OCR 입력 파이프라인 단일 진입점

    목적:
    - 외부 입력(image path or list of paths)을 JD 분석 가능한 텍스트로 변환
    - OCR 품질 상태를 함께 반환
    - 여러 이미지(페이지)인 경우 결과를 병합하여 반환

    처리 흐름:
    1. 입력 정규화 (Single str -> List[str])
    2. 각 이미지별 _process_single_image 실행
    3. 결과 병합 (Text, Lines, Confidence)
    """

    # 1. 입력 정규화
    image_paths = normalize_image_paths(image_input)

    if not image_paths:
        return _fail("no image paths provided")

    # 2. 이미지별 처리 및 수집
    aggregated_raw_texts = []
    aggregated_lines = []
    confidences = []
    errors = []



    for path in image_paths:
        result = _process_single_image(path)

        if result.get("error"):
            errors.append({
                "path": path,
                "error": result["error"],
            })
            continue
        
        # 실패했더라도 부분 결과가 있을 수 있으므로 병합 진행
        # (완전 실패 시 rawText="", lines=[]일 것임)
        if result["rawText"]:
            aggregated_raw_texts.append(result["rawText"])
        
        if result["lines"]:
            aggregated_lines.extend(result["lines"])
            
        # Confidence는 성공/실패 무관하게 계산된 값이면 수집 (0.0 제외?)
        if result["confidence"] > 0:
            confidences.append(result["confidence"])

    # 3. 결과 병합
    final_raw_text = "\n\n".join(aggregated_raw_texts)
    
    # 평균 Confidence 계산
    final_confidence = sum(confidences) / len(confidences) if confidences else 0.0
    
    # 최종 Status 판정 (Enum → str 변환)
    final_status = classify_confidence(final_confidence).value

    return {
        "rawText": final_raw_text,     # 사람이 읽는 용도 (페이지 구분됨)
        "lines": aggregated_lines,     # 👉 JD 파이프라인 입력용 (순서대로 연결)
        "confidence": final_confidence,
        "status": final_status,
        "errors": errors,
    }

def _fail(reason: str) -> dict:
    return {
        "rawText": "",
        "lines": [],
        "confidence": 0.0,
        "status": "FAIL",
        "error": reason,
    }

def _process_single_image(image_path: str) -> dict:

    # ==================================================
    # 0️⃣ 입력 경로 검증 (중요)
    # ==================================================
    if not isinstance(image_path, str):
        return _fail("image_path is not a string")

    if not os.path.exists(image_path):
        return _fail(f"image not found: {image_path}")

    if not os.path.isfile(image_path):
        return _fail(f"image_path is not a file: {image_path}")

    if not os.access(image_path, os.R_OK):
        return _fail(f"image not readable: {image_path}")

    """
    단일 이미지에 대한 OCR 처리
    """
    # 1 이미지 전처리
    try:
        preprocessed_image = preprocess_image(image_path)
    except Exception as e:
        # 이미지 로드 실패 등
        return _fail(f"preprocess_image failed: {e}")

    # 2️ OCR 실행
    ocr_result = run_ocr(preprocessed_image)
    if not ocr_result.get("raw"):
        return _fail("ocr returned empty raw result")

    logger.debug(
        "OCR raw result",
        extra={
            "raw_line_count": len(ocr_result["raw"]),
            "confidence": round(ocr_result.get("confidence", 0), 2),
        },
    )

    # 2.5️⃣ OCR RAW → LINE 구조화 (아직 가공 없음)
    lines = build_lines(ocr_result["raw"])

    logger.debug("OCR lines built", extra={"line_count": len(lines)})

    # 3️⃣ 헤더 감지
    lines = detect_visual_headers(lines)
    # print("\n=== [DEBUG 3] AFTER detect_visual_headers ===")
    # for i, line in enumerate(lines):
    #     header_flag = line.get("is_header")
    #     print(f"[{i:03d}] '{line.get('text', '')}' header={header_flag}")
    # print("============================================\n")

    # 4. 라인 단위 normalize 파이프라인
    normalized = normalize_lines(lines)

    # ⭐ 4.5 라인 단위 품질 게이트 (초기 차단)
    # - confidence 낮은 라인
    # - garbage 비율 높은 라인
    # - 좌표 이상 라인 제거/격리
    passed_lines, dropped_lines = filter_low_quality_lines(
        normalized,
        min_confidence=45,          # 임시 기준
        max_garbage_ratio=0.6       # 임시 기준
    )

    # dump_tmp_data("pass_lines", passed_lines)
    # dump_tmp_data("dropped_lines", dropped_lines)

    # 5. ocr로 처리한 raw 데이터 후처리
    ocr_lines = postprocess_ocr_lines(passed_lines)

    logger.debug(
        "OCR postprocess completed",
        extra={
            "passed_line_count": len(ocr_lines),
            "dropped_line_count": len(dropped_lines),
        },
    )

    # 6. 최종 rawText 생성
    raw_text = build_raw_text(ocr_lines)

    # 7. OCR 품질 상태 분류 (Enum → str 변환)
    status = classify_confidence(ocr_result["confidence"]).value

    return {
        "rawText": raw_text,     # 사람이 읽는 용도
        "lines": ocr_lines,       # 👉 JD 파이프라인 입력용 (중요)
        "confidence": ocr_result["confidence"],
        "status": status,
    }

def normalize_image_paths(image_input) -> list[str]:
    """
    image_input 정규화

    허용 포맷:
    - str (comma-separated)
    - list[str]
    - list[str] where element itself contains comma
    """

    paths: list[str] = []

    if isinstance(image_input, str):
        paths = image_input.split(",")

    elif isinstance(image_input, list):
        for item in image_input:
            if not item:
                continue

            if "," in item:
                paths.extend(item.split(","))
            else:
                paths.append(item)

    return [p.strip() for p in paths if p.strip()]
