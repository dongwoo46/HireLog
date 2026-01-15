from typing import Dict, List
from common.section.loader import load_section_keywords

DEFAULT_SECTION = "summary"
MAX_SUMMARY_LINES = 5


from typing import Dict, List
from common.section.loader import load_section_keywords

# 섹션이 감지되지 않은 라인이 기본적으로 들어갈 섹션
DEFAULT_SECTION = "summary"

# summary 섹션에 허용할 최대 라인 수
# → JD 상단 요약부가 과도하게 길어지는 것을 방지
MAX_SUMMARY_LINES = 5


def build_sections(lines: List[Dict]) -> Dict[str, object]:
    """
    정제된 JD 라인들을 의미 기반 섹션 단위로 구조화한다.

    입력:
    - lines: OCR/정규화 이후의 라인 리스트
      예: [{ "text": "담당 업무" }, { "text": "백엔드 API 개발" }, ...]

    출력:
    - sections: 섹션별 텍스트 묶음
    - canonical_text: 중복 체크 / 요약용으로 사용하는 정렬된 전체 텍스트
    """


    # 결과 섹션 딕셔너리
    # - 최소한 summary는 항상 존재
    sections: Dict[str, List[str]] = {
        DEFAULT_SECTION: []
    }

    # 현재 누적 중인 섹션 상태
    current_section = DEFAULT_SECTION

    # summary 섹션에 몇 줄이 들어갔는지 카운트
    summary_count = 0

    # JD의 각 라인을 순차적으로 처리
    for line in lines:
        # 라인에서 텍스트만 추출
        text = line.get("text", "").strip()

        # 빈 라인은 무시
        if not text:
            continue

        section_hint = line.get("section_hint")

        if section_hint:
            current_section = section_hint
            sections.setdefault(current_section, [])
            continue


        # 2️⃣ summary 섹션 제한 처리
        # - 섹션 헤더가 등장하기 전 텍스트는 summary로 간주
        # - 최대 MAX_SUMMARY_LINES까지만 허용
        if current_section == DEFAULT_SECTION:
            if summary_count < MAX_SUMMARY_LINES:
                sections[DEFAULT_SECTION].append(text)
                summary_count += 1
            continue

        # 3️⃣ 일반 body 라인 누적
        # - 현재 활성화된 섹션에 그대로 추가
        sections[current_section].append(text)

    # 비어 있는 섹션 제거
    sections = {k: v for k, v in sections.items() if v}

    return {
        "sections": sections,
        # 중복 체크 / 요약 / embedding 용 canonical text 생성
        "canonical_text": _build_canonical_text(sections),
    }


def _build_canonical_text(sections: Dict[str, List[str]]) -> str:
    """
    섹션별 텍스트를 고정된 순서로 병합하여
    canonical_text를 생성한다.

    목적:
    - JD 중복 체크
    - 요약 입력
    - 임베딩 생성 시 입력 일관성 확보
    """

    # 의미적으로 중요한 섹션 우선 순서
    ordered = [
        "summary",
        "responsibilities",
        "requiredQualifications",
        "preferredQualifications",
    ]

    lines: list[str] = []

    # 지정된 순서대로 섹션을 펼쳐서 병합
    for key in ordered:
        lines.extend(sections.get(key, []))

    # 줄 단위로 합쳐 하나의 canonical 텍스트 생성
    return "\n".join(lines)

