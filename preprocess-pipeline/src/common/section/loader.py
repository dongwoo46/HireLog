from functools import lru_cache
from pathlib import Path
import yaml

DEFAULT_SECTION_KEYWORDS_PATH = (Path(__file__).parent / "section_keywords.yml")
DEFAULT_META_KEYWORDS_PATH = Path(__file__).parent / "jd_meta_keywords.yml"
DEFAULT_HEADER_KEYWORDS_PATH = Path(__file__).parent / "header_keywords.yml"


def clear_keyword_cache():
    """
    모든 키워드 캐시를 클리어한다.
    yml 파일 수정 후 반영이 필요할 때 사용.
    """
    load_section_keywords.cache_clear()
    load_jd_meta_keywords.cache_clear()
    load_header_keywords.cache_clear()

@lru_cache(maxsize=1)
def load_section_keywords(
        path: Path = DEFAULT_SECTION_KEYWORDS_PATH,
) -> dict[str, list[str]]:
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

@lru_cache(maxsize=1)
def load_jd_meta_keywords(
        path: Path = DEFAULT_META_KEYWORDS_PATH,
) -> set[str]:
    """
    JD 메타 키워드 로드

    - 전형절차 / 고용형태 / 채용 정보 등
    - 실행 중 변경되지 않는 정책 데이터
    - 프로세스 생명주기 동안 1회만 로드
    """

    with open(path, "r", encoding="utf-8") as f:
        raw = yaml.safe_load(f)

    # 1️⃣ YAML이 비어 있거나 dict가 아니면 빈 집합
    if not isinstance(raw, dict):
        return set()

    keywords = raw.get("meta_keywords", [])

    # 2️⃣ meta_keywords가 list가 아니면 무시
    if not isinstance(keywords, list):
        return set()

    # 3️⃣ 문자열만 허용 + 소문자 정규화
    return {
        k.lower()
        for k in keywords
        if isinstance(k, str)
    }


@lru_cache(maxsize=1)
def load_header_keywords(
        path: Path = DEFAULT_HEADER_KEYWORDS_PATH,
) -> set[str]:
    """
    JD 헤더 판별용 키워드 로드

    - 섹션 제목(header) 판정에만 사용
    - 실행 중 변경되지 않는 정책 데이터
    - 프로세스 생명주기 동안 1회만 로드

    반환 예:
    {
      "자격요건",
      "requirements",
      "responsibilities",
      ...
    }
    """

    with open(path, "r", encoding="utf-8") as f:
        raw = yaml.safe_load(f)

    if not isinstance(raw, dict):
        raise ValueError("header_keywords.yml must be a dict")

    keywords = raw.get("header_keywords", [])
    if not isinstance(keywords, list):
        raise ValueError("header_keywords must be a list")

    # 비교 단순화를 위해 전부 소문자 + trim
    return {kw.lower().strip() for kw in keywords if kw}