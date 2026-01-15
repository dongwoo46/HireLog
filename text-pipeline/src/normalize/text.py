import re
import unicodedata

def normalize_text(text: str) -> str:
    """
    입력 텍스트 전체를 공통 규칙으로 정규화한다.

    사용 위치:
    - OCR 결과 텍스트
    - URL/HTML 추출 텍스트
    - PDF, 문서 복사 텍스트

    책임:
    - 유니코드 정규화
    - 공백 정리
    """

    # 전각/반각, 호환 문자 통합
    text = unicodedata.normalize("NFKC", text)

    # 연속 공백/개행을 단일 공백으로 축소
    text = re.sub(r"\s+", " ", text)

    return text.strip()
