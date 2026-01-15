# common/token/canonical.py
import re

def to_canonical(token: str) -> str:
    """
    토큰을 비교용 canonical form으로 변환한다.

    목적:
    - 대소문자 제거
    - 공백/기호 제거
    - OCR 분절 흡수
    """

    token = token.lower()
    token = re.sub(r"[\s\-_.]", "", token)   # g-rpc, g rpc → grpc
    token = re.sub(r"[^a-z0-9]", "", token)

    return token
