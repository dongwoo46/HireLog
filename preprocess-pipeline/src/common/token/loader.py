from functools import lru_cache
from pathlib import Path

import yaml

DEFAULT_PATTERN_PATH = Path(__file__).parent / "patterns.yml"


@lru_cache(maxsize=1)
def load_token_patterns(
        path: Path = DEFAULT_PATTERN_PATH,
) -> dict[str, set[str]]:
    """
    기술 키워드 토큰 보정 패턴 로드

    - 프로세스 생명주기 동안 1회 로드
    - OCR 파이프라인 정책은 실행 중 변경되지 않음
    """

    with open(path, "r", encoding="utf-8") as f:
        raw = yaml.safe_load(f)

    return {
        target: {c.lower() for c in candidates}
        for target, candidates in raw.items()
    }
