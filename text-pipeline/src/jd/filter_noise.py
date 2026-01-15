def filter_jd_noise_lines(
        lines,
        noise_keywords
):
    """
    JD 처리 단계에서 '의미/도메인 관점의 노이즈'를 제거한다.

    이 함수의 책임:
    - JD 문서로서 의미 없는 라인 제거
    - UI / 메타 / boilerplate 문구 제거
    - OCR confidence는 고려하지 않음

    제거 대상:
    - 지원하기, apply, privacy 등 UI/메타 문구
    - JD 본문과 무관한 시스템 문장
    """

    # JD 의미 필터를 통과한 라인만 누적
    result = []

    # JD 처리 대상 라인 순회
    for line in lines:

        # 라인 텍스트 추출 및 공백 제거
        text = line["text"].strip()

        # 키워드 매칭을 위한 소문자 변환
        text_lower = text.lower()

        # 1️⃣ UI / 메타 / boilerplate 키워드 포함 여부 검사
        # → JD 본문과 무관한 문장 제거
        if any(keyword in text_lower for keyword in noise_keywords):
            continue

        # JD 의미 기준을 통과한 라인만 추가
        result.append(line)

    # JD noise 제거가 완료된 라인 목록 반환
    return result
