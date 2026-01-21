"""
Section Builder

역할:
- Core Preprocessing 결과(List[str])를
  '섹션 단위 구조'로 변환한다.

설계 원칙:
- 의미 해석 ❌
- Required / Preferred 판단 ❌
- LLM 친화적 구조 생성이 목적
- 오직 레이아웃 기반(header / bullet)으로만 섹션 분리
"""

from dataclasses import dataclass, field
from .header_detector import is_text_header_candidate


@dataclass
class Section:
    """
    JD의 논리적 섹션 단위
    Section
    ├─ header        → 섹션 이름 (구조)
    ├─ lines         → 설명/서술 텍스트 (문맥)
    ├─ lists         → 항목 나열 (요건/업무/혜택)
    └─ semantic_zone → 의미 태그 (나중에 붙임)

    header:
        - 섹션 제목 (없을 수도 있음)
        - 예: "주요업무", "자격요건", "Requirements"

    lines:
        - 일반 본문 라인
        - bullet을 제외한 설명 문장들

    lists:
        - bullet list 묶음
        - 연속된 bullet들을 하나의 리스트로 그룹화
        - 예: [["item1", "item2"], ["itemA", "itemB"]]

    semantic_zone:
        - Semantic Preprocessing(Lite) 단계에서 부여되는 의미 영역
        - 기본값은 'others'
    """
    header: str | None
    lines: list[str]
    lists: list[list[str]]
    semantic_zone: str = field(default="others")

def build_sections(lines: list[str]) -> list[Section]:
    """
    Core Preprocessing 결과(List[str])를
    레이아웃 기반 Section 구조로 변환한다.

    핵심 원칙:
    - 의미 해석을 하지 않는다
    - 키워드 매칭을 하지 않는다
    - 오직 header / non-header 구조만 분리한다
    """

    # 최종 결과 섹션 목록
    sections: list[Section] = []

    # 현재 처리 중인 섹션
    # JD 초반에는 header가 없는 경우가 많으므로 intro 섹션으로 시작
    current = Section(
        header=None,
        lines=[],
        lists=[],
        semantic_zone="intro"   # ← 여기
    )

    for idx, line in enumerate(lines):
        # 공백 제거 (판정 안정성 목적, 의미 변경 X)
        stripped = line.strip()

        # header 판정을 위한 lookahead
        next_line = lines[idx + 1] if idx + 1 < len(lines) else None

        # 1️⃣ Header 판정
        # 의미가 아니라 "레이아웃 상 섹션 제목처럼 보이는지"만 판단
        if is_text_header_candidate(stripped, next_line):
            # 기존 섹션에 내용이 있다면 결과에 추가
            if current.lines or current.lists:
                sections.append(current)

            # 새로운 섹션 시작
            current = Section(
                header=normalize_header(stripped),
                lines=[],
                lists=[]
            )
            continue

        # 2️⃣ Non-header 라인 처리
        # 의미 판단 없이 그대로 lines에 보존
        if stripped:
            current.lines.append(stripped)

    # 루프 종료 후 마지막 섹션 정리
    if current.lines or current.lists:
        sections.append(current)

    return sections

def normalize_header(header: str | None) -> str | None:
    """
    Header canonicalizer (aggressive)

    정책:
    - header에 한해서
    - 모든 공백 제거
    - 의미 해석 ❌
    - canonical key 생성이 목적
    """

    if header is None:
        return None

    return header.replace(" ", "")



