"""
Semantic Zone Detector (Lite)

역할:
- Section.header 문자열을 기반으로
  섹션의 의미 영역(semantic zone)을 판별한다.

중요:
- 내용(lines, lists) 절대 참조 ❌
- 키워드 기반 "약한 의미 힌트"만 제공
- 오탐보다 미탐을 허용
- section_keywords.yml에서 키워드 로드
"""

from typing import Optional
from common.section.loader import load_section_keywords


# section_keywords.yml의 키를 semantic_zone 이름으로 매핑
_SECTION_KEYWORD_MAP = {
    "summary": "company",
    "responsibilities": "responsibilities",
    "requiredQualifications": "requirements",
    "preferredQualifications": "preferred",
    "skills": "skills",
    "experience": "experience",
    "recruitmentProcess": "process",
    "applicationGuide": "application_guide",  # 토스 스타일 지원 가이드
    "employmentType": "employment_type",
    "location": "location",
    "benefits": "benefits",
    "etc": "others",
}

# section_keywords.yml 로드 (lazy)
_section_keywords: Optional[dict[str, list[str]]] = None


def _get_section_keywords() -> dict[str, list[str]]:
    """섹션 키워드 로드 (lazy)"""
    global _section_keywords
    if _section_keywords is None:
        _section_keywords = load_section_keywords()
    return _section_keywords


def _get_keywords_for_zone(zone_name: str) -> list[str]:
    """특정 semantic zone에 해당하는 키워드 리스트 반환"""
    keywords_map = _get_section_keywords()
    result = []
    
    # section_keywords.yml의 키를 semantic_zone으로 매핑
    for section_key, zone in _SECTION_KEYWORD_MAP.items():
        if zone == zone_name and section_key in keywords_map:
            result.extend(keywords_map[section_key])
    
    return result


# 각 zone별 키워드 (lazy 로드)
def _get_responsibilities_keywords() -> tuple:
    return tuple(_get_keywords_for_zone("responsibilities"))


def _get_requirements_keywords() -> tuple:
    return tuple(_get_keywords_for_zone("requirements"))


def _get_preferred_keywords() -> tuple:
    return tuple(_get_keywords_for_zone("preferred"))


def _get_company_keywords() -> tuple:
    return tuple(_get_keywords_for_zone("company"))


def _get_benefits_keywords() -> tuple:
    return tuple(_get_keywords_for_zone("benefits"))


def _get_process_keywords() -> tuple:
    return tuple(_get_keywords_for_zone("process"))


def _get_experience_keywords() -> tuple:
    return tuple(_get_keywords_for_zone("experience"))


def _get_skills_keywords() -> tuple:
    return tuple(_get_keywords_for_zone("skills"))


def _get_employment_type_keywords() -> tuple:
    return tuple(_get_keywords_for_zone("employment_type"))


def _get_location_keywords() -> tuple:
    return tuple(_get_keywords_for_zone("location"))


def _get_application_guide_keywords() -> tuple:
    return tuple(_get_keywords_for_zone("application_guide"))


# 지원서 질문은 section_keywords.yml에 없으므로 하드코딩 유지
APPLICATION_QUESTION_KEYWORDS = (
    "공통질문",
    "자기소개서",
    "지원서 문항",
    "application question",
    "common question",
)

def _normalize_for_match(text: str) -> str:
    """
    매칭용 텍스트 정규화
    - 공백 제거
    - 소문자 변환
    """
    return text.lower().replace(" ", "").strip()


def _matches_keywords(header: str, keywords: tuple) -> bool:
    """
    헤더가 키워드와 매칭되는지 검사
    - 둘 다 공백 제거 후 비교
    - 부분 일치 허용
    """
    h_normalized = _normalize_for_match(header)

    for kw in keywords:
        kw_normalized = _normalize_for_match(kw)
        if kw_normalized in h_normalized or h_normalized in kw_normalized:
            return True

    return False


def detect_semantic_zone(header: str | None) -> str:
    """
    Semantic Zone Detector (Lite)

    역할:
    - 섹션 header 문자열만을 기준으로
      JD 섹션의 의미 영역(semantic zone)을 판별한다.

    설계 원칙:
    - header 이외의 내용(lines, lists)은 절대 참조하지 않는다.
    - 키워드 기반의 '약한 의미 힌트'만 사용한다.
    - 오탐(false positive)보다 미탐(false negative)을 허용한다.
    - 판단 우선순위는 코드의 if 순서 자체에 의미를 둔다.

    반환 값:
    - responsibilities
    - requirements
    - preferred
    - experience
    - company
    - benefits
    - application_questions
    - process
    - others
    """

    # header가 없는 경우:
    # - 타이틀 없는 섹션
    # - 메타/잡음 섹션
    # → 의미 판별 불가로 others 처리
    if header is None:
        return "others"

    h = header  # _matches_keywords에서 정규화 처리
    h_norm = _normalize_for_match(h)

    # Hard fallback rules for common KR headers.
    # (Used when keyword resources are noisy or partial.)
    if any(k in h_norm for k in ("주요업무", "담당업무", "업무내용", "직무")):
        return "responsibilities"
    if any(k in h_norm for k in ("자격요건", "필수요건", "지원자격", "요구사항")):
        return "requirements"
    if any(k in h_norm for k in ("우대사항", "우대조건", "가산점")):
        return "preferred"
    if any(k in h_norm for k in ("기술스택", "techstack", "stack", "skills")):
        return "skills"
    if any(k in h_norm for k in ("채용절차", "전형절차", "채용과정")):
        return "process"

    # 1️⃣ 주요 업무 / 역할
    # JD에서 가장 핵심 영역이므로 최우선 판별
    if _matches_keywords(h, _get_responsibilities_keywords()):
        return "responsibilities"

    # 2️⃣ 우대 사항
    # 'preferred'가 명시되면 qualification 포함 여부와 무관하게 우대
    if _matches_keywords(h, _get_preferred_keywords()):
        return "preferred"

    # 3️⃣ 필수 자격 요건
    # preferred에 해당하지 않는 qualification / requirement만 처리
    if _matches_keywords(h, _get_requirements_keywords()):
        return "requirements"

    # 4️⃣ 경력 / 경험
    # 자격요건과 구분하여 별도 관리
    # "경력 3년 이상", "신입 가능" 등
    if _matches_keywords(h, _get_experience_keywords()):
        return "experience"

    # 5️⃣ 회사 / 포지션 소개
    # JD 요약에는 직접 사용되지 않지만
    # 메타 정보 분리를 위해 구분
    if _matches_keywords(h, _get_company_keywords()):
        return "company"

    # 6️⃣ 복지 / 보상
    # JD 본문과 분리하여 관리
    if _matches_keywords(h, _get_benefits_keywords()):
        return "benefits"

    # 7️⃣ 지원서 / 공통 질문
    # 채용 절차와 의미가 다르므로 process보다 우선 판별
    if _matches_keywords(h, APPLICATION_QUESTION_KEYWORDS):
        return "application_questions"

    # 7️⃣-2 지원 가이드 (토스 스타일)
    # "이력서는 이렇게 작성하시는 걸 추천해요" 등
    if _matches_keywords(h, _get_application_guide_keywords()):
        return "application_guide"

    # 8️⃣ 채용 절차
    # 인터뷰, 전형 단계 등
    if _matches_keywords(h, _get_process_keywords()):
        return "process"

    # 9️⃣ 기술 스택
    # 기술 요건, 사용 기술 등
    if _matches_keywords(h, _get_skills_keywords()):
        return "skills"

    # 🔟 고용 형태
    # 정규직, 계약직, 인턴 등
    if _matches_keywords(h, _get_employment_type_keywords()):
        return "employment_type"

    # 1️⃣1️⃣ 근무지
    # 위치, 오피스 등
    if _matches_keywords(h, _get_location_keywords()):
        return "location"

    # 위 조건에 해당하지 않는 경우:
    # - 분류 가치가 낮은 섹션
    # - 추후 처리 가능한 영역
    return "others"
