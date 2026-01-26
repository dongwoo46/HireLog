import re
from dataclasses import replace
from .section_builder import Section

# bullet 형태의 라인을 감지하기 위한 정규식
# - 허용 기호: • - * · – ▪
# - 의미 해석 없이 "목록처럼 보이는 형태"만 판별
# - 실제 항목 내용은 그룹(1)으로 캡처
_BULLET_RE = re.compile(r"^[\s]*[•\-*·–▪]\s+(.*)")


def group_lists(section: Section) -> Section:
    """
    Section 내부의 lines를 순회하며
    bullet 형태의 라인들을 감지해 lists로 그룹화한다.

    동작 원칙:
    - bullet 판정은 오직 정규식 기반 (레이아웃)
    - 연속된 bullet 라인만 하나의 리스트로 묶는다
    - bullet이 아닌 라인은 lines에 그대로 유지한다
    - 의미 해석 / 요건 분류는 절대 수행하지 않는다
    - immutable 방식으로 새 Section 객체 반환
    """

    # 최종 bullet 리스트 묶음
    lists = []

    # 현재 누적 중인 bullet 그룹
    current_list = []

    # bullet 제거 후 남길 일반 라인들
    new_lines = []

    for line in section.lines:
        # bullet 패턴 매칭 시도
        m = _BULLET_RE.match(line)

        if m:
            # bullet 항목인 경우
            # 기호를 제거한 실제 텍스트만 저장
            current_list.append(m.group(1).strip())
        else:
            # bullet이 아닌 라인 등장 시
            # 누적 중이던 bullet 그룹이 있다면 확정
            if current_list:
                lists.append(current_list)
                current_list = []

            # 일반 라인은 그대로 유지
            new_lines.append(line)

    # 루프 종료 후 남아 있는 bullet 그룹 처리
    if current_list:
        lists.append(current_list)

    # immutable 방식: dataclasses.replace로 새 객체 반환
    return replace(section, lines=new_lines, lists=lists)
