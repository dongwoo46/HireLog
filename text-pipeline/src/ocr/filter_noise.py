from common.section.loader import load_jd_meta_keywords

def filter_ocr_noise_lines(
        lines,
        noise_keywords
):
    """
    JD 처리 단계에서 '의미/도메인 관점의 노이즈'를 제거한다.

    이 함수의 책임:
    - JD 문서로서 의미 없는 라인 제거

    제거 대상:
    - 지원하기, apply, privacy 등 UI/메타 문구
    - JD 본문과 무관한 시스템 문장
    """

    # JD 의미 필터를 통과한 라인만 누적
    result: list[dict] = []

    # JD 필요 데이터 로드
    meta_keywords = load_jd_meta_keywords()

    # JD 처리 대상 라인 순회
    for line in lines:

        # 라인 텍스트 추출 및 공백 제거
        text = line["text"].strip()
        if not text:
            continue

        # 키워드 매칭을 위한 소문자 변환
        text_lower = text.lower()

        # #  JD 메타 라인은 무조건 통과
        if any(k in text_lower for k in meta_keywords):
            result.append(line)
            continue

        #  ️UI / 메타 / boilerplate 키워드 포함 여부 검사
        # → JD 본문과 무관한 문장 제거
        if _is_noise_line(text, noise_keywords):
            continue

        # JD 의미 기준을 통과한 라인만 추가
        result.append(line)

    # JD noise 제거가 완료된 라인 목록 반환
    return result

def _is_noise_line(text: str, noise_keywords: dict[str, set[str]]) -> bool:
    text_lower = text.lower().strip()

    # 1️⃣ exact match
    if text_lower in noise_keywords.get("exact", set()):
        return True

    # 2️⃣ prefix match
    for p in noise_keywords.get("prefix", set()):
        if text_lower.startswith(p):
            return True

    # 3️⃣ suffix match
    for s in noise_keywords.get("suffix", set()):
        if text_lower.endswith(s):
            return True

    # 4️⃣ navigation / 포함 match
    for n in noise_keywords.get("navigation", set()):
        if n in text_lower:
            return True

    return False
