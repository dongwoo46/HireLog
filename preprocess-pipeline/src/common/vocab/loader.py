from functools import lru_cache
from pathlib import Path
import yaml

DEFAULT_JD_VOCAB_PATH = Path(__file__).parent / "jd_vocab.yml"
DEFAULT_SKILL_ALIAS_PATH = Path(__file__).parent / "skill_alias.yml"
DEFAULT_SKILL_VOCAB_PATH = Path(__file__).parent / "skill_vocab.yml"


@lru_cache(maxsize=1)
def load_jd_vocab(
        path: Path = DEFAULT_JD_VOCAB_PATH,
) -> set[str]:
    """
    JD 도메인에서 '절대 보호해야 하는 단어 집합' 로드

    - 프로세스 생명주기 동안 1회 로드
    - 정책 파일(jd_vocab.yml)은 실행 중 변경되지 않음
    - 소문자 정규화 + 중복 제거
    """

    with open(path, "r", encoding="utf-8") as f:
        raw = yaml.safe_load(f)

    if not isinstance(raw, dict):
        raise ValueError("jd_vocab.yml must be a mapping of groups")

    vocab: set[str] = set()

    for _, tokens in raw.items():
        if not isinstance(tokens, list):
            continue

        for token in tokens:
            if isinstance(token, str):
                vocab.add(token.lower().strip())

    return vocab






@lru_cache(maxsize=1)
def load_skill_vocab(
        path: Path = DEFAULT_SKILL_VOCAB_PATH,
) -> dict[str, set[str]]:
    """
    JD 기술 스택 추출용 vocab 로드

    구조:
    {
      "language": ["java", "kotlin", ...],
      "framework": ["spring", "django", ...],
      ...
    }

    정책:
    - 프로세스 생명주기 동안 1회 로드
    - 실행 중 정책 파일 변경 없음
    - 소문자 정규화 + 중복 제거
    """

    with open(path, "r", encoding="utf-8") as f:
        raw = yaml.safe_load(f)

    if not isinstance(raw, dict):
        raise ValueError("skill_vocab.yml must be a mapping of categories")

    vocab: dict[str, set[str]] = {}

    for category, tokens in raw.items():
        if not isinstance(tokens, list):
            continue

        normalized: set[str] = set()

        for token in tokens:
            if isinstance(token, str):
                normalized.add(token.lower().strip())

        if normalized:
            vocab[category] = normalized

    return vocab



@lru_cache(maxsize=1)
def load_skill_alias(
        path: Path = DEFAULT_SKILL_ALIAS_PATH,
) -> dict[str, str]:
    """
    skill alias 로드
    - alias → canonical 매핑
    - 파일이 없거나 비어 있으면 빈 dict 반환
    """

    if not path.exists():
        return {}

    with open(path, "r", encoding="utf-8") as f:
        raw = yaml.safe_load(f)

    if not raw:
        # None, {}, 빈 파일 모두 허용
        return {}

    if not isinstance(raw, dict):
        raise ValueError("skill_alias.yml must be a dict")

    alias_map: dict[str, str] = {}

    for canonical, aliases in raw.items():
        if not isinstance(aliases, list):
            continue

        canonical_norm = canonical.lower().strip()

        for alias in aliases:
            if isinstance(alias, str):
                alias_map[alias.lower().strip()] = canonical_norm

    return alias_map
