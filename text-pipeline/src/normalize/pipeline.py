from typing import List, Dict

from normalize.text import normalize_text
from normalize.char import normalize_chars
from normalize.line import normalize_line


def normalize_lines(lines: List[Dict]) -> List[Dict]:
    """
    OCR 라인 리스트에 normalize 파이프라인을 적용한다.

    책임:
    - 문서 공통 정규화 (NFKC, 공백)
    - 문자 단위 normalize
    - 토큰 단위 normalize (내부 정책 사용)
    - 라인 단위 normalize
    """

    result = []

    for line in lines:
        # 0️⃣ 문서 공통 정규화
        text = normalize_text(line["text"])

        # 1️⃣ 문자 단위 normalize
        text = normalize_chars(text)

        # 3️⃣ 라인 단위 normalize (의미 없는 라인 제거)
        if normalize_line({"text": text}) is None:
            continue

        # 4️⃣ text만 교체하고 metadata는 유지
        result.append({
            **line,
            "text": text
        })

    return result
