from typing import List, Dict

from normalize.text import normalize_text
from normalize.char import normalize_chars
from normalize.line import normalize_line


def normalize_lines(lines: List[Dict]) -> List[Dict]:
    """
    OCR 결과 line 리스트에 대해 텍스트 정규화 파이프라인을 적용한다.

    이 함수의 역할:
    - OCR 엔진 종류와 무관하게 동작한다.
    - line 단위 텍스트를 대상으로
      공통 정규화 → 문자 정규화 → 라인 정규화를 수행한다.
    - 텍스트만 수정하고, 위치/신뢰도 등의 메타데이터는 유지한다.

    책임 범위:
    - 텍스트 형태 통일
    - OCR 특유의 깨짐 최소화
    - 의미 없는 라인 제거

    책임이 아닌 것:
    - 헤더 판단
    - 품질 필터링
    - JD 도메인 해석
    """

    result = []

    for line in lines:
        original_text = line.get("text", "")

        # 문서 공통 정규화
        # - Unicode NFKC
        # - 공백 / 개행 정리
        text = normalize_text(original_text)

        # 문자 단위 정규화
        # - OCR 오인식 문자 교정
        # - 유사 문자 통일
        text = normalize_chars(text)

        # 라인 단위 정규화
        # - 의미 없는 라인 제거
        # - 단일 특수문자, 잡음 라인 필터링
        if normalize_line({"text": text}) is None:
            continue

        # 텍스트만 교체하고
        # 나머지 메타데이터(confidence, bbox, height 등)는 그대로 유지
        result.append({
            **line,
            "text": text
        })

    return result
