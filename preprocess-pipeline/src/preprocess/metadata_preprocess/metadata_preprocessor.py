"""
Metadata Preprocessor

책임:
- Core Preprocessing 결과(lines)를 순회
- 문서 전역 메타데이터 수집
- 구조 / 의미 로직과 완전 분리
"""

from .document_meta import DocumentMeta
from .date_meta import extract_recruitment_period


class MetadataPreprocessor:
    """
    메타데이터 전용 전처리기

    특징:
    - lines를 수정하지 않음
    - 예외 발생 ❌
    - 항상 DocumentMeta 반환
    """

    def process(self, lines: list[str]) -> DocumentMeta:
        recruitment_period = extract_recruitment_period(lines)

        return DocumentMeta(
            recruitment_period=recruitment_period
        )
