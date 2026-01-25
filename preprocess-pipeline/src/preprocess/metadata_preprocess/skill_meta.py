"""
Skill / Technology Stack Metadata

책임:
- JD 텍스트 라인에서 기술 스택 추출
- skill_vocab.yml과 skill_alias.yml 기반 매칭
- 중복 제거 후 정렬된 리스트로 반환

원칙:
- 대소문자 무시
- 별칭(alias) 정규화
- 정보 손실 ❌
"""

import re
from dataclasses import dataclass
from typing import List, Optional, Dict, Set
from common.vocab.loader import load_skill_vocab, load_skill_alias


# ==================================================
# Data Models
# ==================================================

@dataclass
class SkillMeta:
    """
    개별 기술 스택 발견 정보

    raw_text:
        - 기술 스택이 발견된 원문 라인

    skill_name:
        - 정규화된 기술 스택 이름 (canonical)

    category:
        - 기술 스택 카테고리 (language, backend_framework 등)
    """
    raw_text: str
    skill_name: str
    category: str


@dataclass
class SkillSetMeta:
    """
    기술 스택 집합 메타데이터 (최종 결과)

    skills:
        - 추출된 모든 기술 스택 리스트 (중복 제거, 정렬)
    """
    skills: List[str]


# ==================================================
# Vocab Loading (Lazy)
# ==================================================

_skill_vocab: Optional[Dict[str, Set[str]]] = None
_skill_alias: Optional[Dict[str, str]] = None


def _get_skill_vocab() -> Dict[str, Set[str]]:
    """기술 스택 vocab 로드 (lazy)"""
    global _skill_vocab
    if _skill_vocab is None:
        _skill_vocab = load_skill_vocab()
    return _skill_vocab


def _get_skill_alias() -> Dict[str, str]:
    """기술 스택 별칭 매핑 로드 (lazy)"""
    global _skill_alias
    if _skill_alias is None:
        _skill_alias = load_skill_alias()
    return _skill_alias


# ==================================================
# Extractors
# ==================================================

def extract_skill_metas(line: str) -> List[SkillMeta]:
    """
    단일 라인에서 기술 스택 전부 추출

    정책:
    - 대소문자 무시
    - 별칭은 정규화하여 canonical 이름으로 변환
    - 단어 경계 고려 (부분 문자열 매칭 방지)
    """
    vocab = _get_skill_vocab()
    alias_map = _get_skill_alias()
    
    line_lower = line.lower()
    results: List[SkillMeta] = []
    found_skills: Set[tuple] = set()  # (category, skill_name) 중복 방지

    # 1️⃣ vocab의 모든 기술 스택과 별칭을 포함한 검색 대상 생성
    # (canonical -> category 매핑)
    canonical_to_category: Dict[str, str] = {}
    for category, skills in vocab.items():
        for skill in skills:
            canonical_to_category[skill] = category

    # 2️⃣ 모든 검색 패턴 생성 (vocab + alias)
    search_patterns: List[tuple] = []  # (pattern, canonical, category)
    
    def _make_pattern(skill: str) -> str:
        """
        기술 스택 이름을 검색 패턴으로 변환
        - 점(.)이 포함된 경우 특별 처리
        - 단어 경계 고려하되 점 앞뒤는 유연하게 처리
        """
        escaped = re.escape(skill)
        # 점이 포함된 경우: 앞뒤가 단어 문자가 아니거나 시작/끝인 경우
        if '.' in skill:
            # 점 앞뒤는 단어 경계가 필요 없음 (점 자체가 구분자 역할)
            return r'(?<!\w)' + escaped + r'(?!\w)'
        else:
            # 일반적인 경우: 단어 경계 사용
            return r'\b' + escaped + r'\b'
    
    # vocab의 canonical 기술 스택
    for canonical, category in canonical_to_category.items():
        pattern = _make_pattern(canonical)
        search_patterns.append((pattern, canonical, category))
    
    # 별칭 매핑
    for alias, canonical in alias_map.items():
        if canonical in canonical_to_category:
            category = canonical_to_category[canonical]
            pattern = _make_pattern(alias)
            search_patterns.append((pattern, canonical, category))

    # 3️⃣ 라인에서 모든 패턴 검색
    for pattern, canonical, category in search_patterns:
        if re.search(pattern, line_lower, re.IGNORECASE):
            if (category, canonical) not in found_skills:
                results.append(
                    SkillMeta(
                        raw_text=line,
                        skill_name=canonical,
                        category=category
                    )
                )
                found_skills.add((category, canonical))

    return results


def extract_skill_set(lines: List[str]) -> SkillSetMeta:
    """
    JD 전체 라인을 기준으로 기술 스택 집합 메타데이터 결정

    정책:
    - 모든 라인에서 기술 스택 수집
    - 중복 제거
    - 정렬된 리스트로 반환
    """
    skill_metas: List[SkillMeta] = []

    # 1️⃣ 모든 라인에서 기술 스택 수집
    for line in lines:
        metas = extract_skill_metas(line)
        if metas:
            skill_metas.extend(metas)

    # 2️⃣ 중복 제거 및 정렬
    unique_skills: Set[str] = set()
    for meta in skill_metas:
        unique_skills.add(meta.skill_name)

    return SkillSetMeta(
        skills=sorted(unique_skills)
    )
