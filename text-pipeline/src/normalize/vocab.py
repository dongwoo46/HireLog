from common.token.canonical import to_canonical


def is_known_vocab(token: str, vocab: set[str]) -> bool:
    """
    토큰이 JD 도메인에서 '확실한 정상 단어'인지 판별한다.

    정책:
    - 애매하면 False
    - 완전 일치만 True
    - 깨진 단어 보호 금지 (jav4 ≠ java)

    예:
    - java → True
    - spring → True
    - jav4 → False
    - javas → False
    """

    if not token:
        return False

    canonical = to_canonical(token)

    # 너무 짧은 토큰은 vocab 보호하지 않음
    # (예외는 vocab에 직접 넣어서 관리)
    if len(canonical) <= 2:
        return canonical in vocab

    return canonical in vocab
