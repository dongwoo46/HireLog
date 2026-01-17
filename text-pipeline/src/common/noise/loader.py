from functools import lru_cache
from pathlib import Path
from typing import Dict, Set

import yaml


DEFAULT_NOISE_KEYWORDS_PATH = (
        Path(__file__).resolve().parent / "noise_keywords.yml"
)


@lru_cache(maxsize=1)
def load_noise_keywords(
        path: str | Path | None = None,
) -> Dict[str, Set[str]]:
    """
    노이즈 키워드 로더 (YAML 전용)

    - UI / 시스템 / 네비게이션 노이즈 정책
    - 실행 중 변경되지 않는 정적 데이터
    - 소문자 + trim 정규화
    - 프로세스 생명주기 동안 1회 로드

    반환 형태:
    {
        "exact": {...},
        "prefix": {...},
        "suffix": {...},
        "navigation": {...}
    }
    """

    if path is None:
        path = DEFAULT_NOISE_KEYWORDS_PATH
    else:
        path = Path(path)

    if not path.exists():
        raise FileNotFoundError(f"noise keywords file not found: {path}")

    with open(path, encoding="utf-8") as f:
        raw = yaml.safe_load(f)

    if not isinstance(raw, dict):
        raise ValueError("noise_keywords.yml must be a dict")

    result: Dict[str, Set[str]] = {}

    for key in ("exact", "prefix", "suffix", "navigation"):
        values = raw.get(key, [])

        if not isinstance(values, list):
            raise ValueError(f"{key} must be a list")

        result[key] = {
            v.lower().strip()
            for v in values
            if isinstance(v, str) and v.strip()
        }

    return result
