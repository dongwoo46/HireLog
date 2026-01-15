import pytesseract
from pytesseract import Output


TESSERACT_CONFIG = "--oem 1 --psm 6"
TESSERACT_LANG = "kor+eng"


def run_ocr(image):
    """
    OCR 엔진 실행 함수

    목적:
    - 전처리된 이미지를 OCR 엔진에 전달
    - 단어 단위 텍스트와 confidence를 수집
    - 후속 파이프라인에서 사용할 raw 데이터 제공
    """

    # pytesseract OCR 실행
    # 단어 단위 결과 + confidence + 위치 정보 반환
    data = pytesseract.image_to_data(
        image,
        lang=TESSERACT_LANG,
        config=TESSERACT_CONFIG,
        output_type=Output.DICT,
    )

    texts = []          # 인식된 단어 텍스트
    confidences = []    # 단어별 confidence 값

    # OCR 결과는 단어 단위로 반환됨
    for i in range(len(data["text"])):
        text = data["text"][i].strip()
        conf = int(data["conf"][i])

        # 인식 실패(conf <= 0) 또는 빈 문자열 제거
        if conf > 0 and text:
            texts.append(text)
            confidences.append(conf)

    # 전체 OCR 평균 confidence 계산
    avg_confidence = (
        sum(confidences) / len(confidences)
        if confidences else 0
    )

    # 후속 처리에 필요한 정보 반환
    return {
        "text": " ".join(texts),   # 디버그용 전체 텍스트
        "confidence": avg_confidence,
        "raw": data                # line 구조화를 위한 원본 데이터
    }
