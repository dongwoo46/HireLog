from typing import List, Dict


def normalize_bullets(lines: List[Dict]) -> List[Dict]:
    """
    JD 라인에서 불릿 기호를 정규화한다.

    역할:
    - '-', '•', '*', '–' 등으로 시작하는 라인을
      동일한 불릿 구조로 통일
    - 이후 섹션 빌더 / feature extractor가
      불릿 여부를 안정적으로 활용할 수 있게 함
    """

    for line in lines:
        text = line.get("text", "").strip()

        if not text:
            continue

        if text.startswith(("-", "•", "*", "–")):
            line["text"] = text.lstrip("-•*– ").strip()
            line["is_bullet"] = True
        else:
            line["is_bullet"] = False

    return lines
