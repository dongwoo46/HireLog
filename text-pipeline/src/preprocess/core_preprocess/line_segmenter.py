# preprocess/core_preprocess/line_segmenter.py

def segment_lines(text: str) -> list[str]:
    """
    텍스트를 라인 단위로 분리한다.

    역할:
    - 개행 기준으로 줄을 나눈다
    - 빈 줄은 제거한다
    - 줄 순서는 그대로 유지한다

    설계 이유:
    - JD 구조 판단에서 '빈 줄'은 의미 정보가 아님
    - Header / Bullet 판단은 인접 라인 관계로 충분
    - 이후 파이프라인 단순화 목적

    ❌ 하지 않는 것:
    - 문장 병합
    - 의미 추론
    """

    if not text:
        return []

    return [
        line.strip()
        for line in text.split("\n")
        if line.strip()
    ]
