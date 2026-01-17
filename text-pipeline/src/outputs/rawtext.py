"""
사람이 읽을 수 있는 최종 JD rawText 생성 모듈
"""

def build_raw_text(lines: list[dict]) -> str:
    """
    OCR / JD 전처리 결과를 사람이 읽기 좋은 텍스트로 변환

    목적:
    - 디버깅
    - 사용자 미리보기
    - JD 여부 판별용 입력
    """

    texts = []

    for line in lines:
        text = line.get("text")
        if text:
            texts.append(text)

    return "\n".join(texts)
