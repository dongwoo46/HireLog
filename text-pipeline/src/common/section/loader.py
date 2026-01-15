from functools import lru_cache
from pathlib import Path
from typing import Dict, List
import yaml

DEFAULT_SECTION_KEYWORDS_PATH = (
        Path(__file__).parent / "section_keywords.yml"
)

@lru_cache(maxsize=1)
def load_section_keywords(
        path: Path = DEFAULT_SECTION_KEYWORDS_PATH,
) -> Dict[str, List[str]]:
    """
    JD 섹션 키워드 로드

    반환 예:
    {
      "responsibilities": ["담당 업무", "주요 업무", ...],
      "requiredQualifications": [...],
      ...
    }
    """

    with open(path, "r", encoding="utf-8") as f:
        raw = yaml.safe_load(f)

    if not isinstance(raw, dict):
        raise ValueError("section_keywords.yml must be a dict")

    # 비교 단순화를 위해 전부 소문자
    return {
        section: [kw.lower().strip() for kw in keywords]
        for section, keywords in raw.items()
        if isinstance(keywords, list)
    }
