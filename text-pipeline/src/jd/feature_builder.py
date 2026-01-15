"""
JD Feature Builder

역할:
- section_builder 결과를 입력으로 받아
- 기술 스택, 요건 텍스트 등을 조합
- LLM 요약 / 중복체크 / 검색의 공통 입력 생성
"""

from typing import Dict
from jd.skill_extractor import extract_skills_from_lines


def build_jd_features(section_result: Dict[str, object]) -> Dict[str, object]:
    sections = section_result["sections"]

    required = sections.get("requiredQualifications", [])
    preferred = sections.get("preferredQualifications", [])

    skills_required = extract_skills_from_lines(required)
    skills_preferred = extract_skills_from_lines(preferred)

    return {
        "skills": {
            "required": skills_required,
            "preferred": skills_preferred,
        },
        "required_text": required,
        "preferred_text": preferred,
    }
