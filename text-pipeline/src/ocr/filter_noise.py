import re

def filter_ocr_noise_lines(
        lines,
        min_avg_conf=40,
        max_low_conf_ratio=0.5
):
    """
    OCR 결과에서 '인식 품질 문제'로 판단되는 라인을 제거한다.

    이 함수의 책임:
    - OCR 인식 실패 또는 품질이 낮은 라인 제거
    - 문서 의미(JD, 문장 구조)는 고려하지 않음
    - 오직 OCR 결과 품질만 기준으로 판단

    제거 대상:
    - 평균 confidence가 낮은 라인
    - low confidence 토큰 비율이 높은 라인
    - OCR 쓰레기(기호/숫자만 있는 라인)
    """

    # OCR 품질 필터를 통과한 라인만 누적
    result = []

    # OCR 파이프라인에서 전달된 모든 라인 순회
    for line in lines:

        # 라인 텍스트의 앞뒤 공백 제거
        text = line["text"].strip()

        # 1️⃣ 평균 confidence 기준 제거
        # → OCR이 해당 라인을 제대로 인식하지 못했을 가능성
        if line["confidence_avg"] < min_avg_conf:
            continue

        # 2️⃣ low confidence 토큰 비율 기준 제거
        # → 일부 토큰만 맞고 나머지가 깨진 라인 제거
        if line["low_conf_ratio"] > max_low_conf_ratio:
            continue

        # 3️⃣ 너무 짧은 라인 제거
        # → 단일 문자, OCR 잔여 토큰 제거
        if len(text) <= 2:
            continue

        # 4️⃣ 특수문자 또는 기호만 있는 라인 제거
        # 예: "---", "|||", "•••"
        if re.fullmatch(r"[\W_]+", text):
            continue

        # 5️⃣ 숫자만 있는 라인 제거
        # → 페이지 번호, UI 카운터 등
        if text.isdigit():
            continue

        # OCR 품질 기준을 통과한 라인만 추가
        result.append(line)

    # OCR noise 제거가 완료된 라인 목록 반환
    return result
