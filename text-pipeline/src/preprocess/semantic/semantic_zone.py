"""
Semantic Zone Detector (Lite)

역할:
- Section.header 문자열을 기반으로
  섹션의 의미 영역(semantic zone)을 판별한다.

중요:
- 내용(lines, lists) 절대 참조 ❌
- 키워드 기반 "약한 의미 힌트"만 제공
- 오탐보다 미탐을 허용
"""

RESPONSIBILITIES_KEYWORDS = (
    "responsibil",
    "role",
    "주요업무",
    "담당업무",
)

REQUIREMENTS_KEYWORDS = (
    "requirement",
    "qualification",
    "자격요건",
)

PREFERRED_KEYWORDS = (
    "preferred",
    "우대사항",
)

COMPANY_KEYWORDS = (
    "company",
    "회사소개",
    "포지션",
    "포지션 상세",
    "about",
)

BENEFITS_KEYWORDS = (
    "benefit",
    "복지",
    "혜택",
    "합격보상",
)

PROCESS_KEYWORDS = (
    "채용 전형",
    "전형 절차",
    "채용절차",
)

APPLICATION_QUESTION_KEYWORDS = (
    "공통질문",
    "자기소개서",
    "지원서 문항",
)

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

    h = header.lower().strip()

    # 1️⃣ 주요 업무 / 역할
    # JD에서 가장 핵심 영역이므로 최우선 판별
    if any(k in h for k in RESPONSIBILITIES_KEYWORDS):
        return "responsibilities"

    # 2️⃣ 우대 사항
    # 'preferred'가 명시되면 qualification 포함 여부와 무관하게 우대
    if any(k in h for k in PREFERRED_KEYWORDS):
        return "preferred"

    # 3️⃣ 필수 자격 요건
    # preferred에 해당하지 않는 qualification / requirement만 처리
    if any(k in h for k in REQUIREMENTS_KEYWORDS):
        return "requirements"

    # 4️⃣ 회사 / 포지션 소개
    # JD 요약에는 직접 사용되지 않지만
    # 메타 정보 분리를 위해 구분
    if any(k in h for k in COMPANY_KEYWORDS):
        return "company"

    # 5️⃣ 복지 / 보상
    # JD 본문과 분리하여 관리
    if any(k in h for k in BENEFITS_KEYWORDS):
        return "benefits"

    # 6️⃣ 지원서 / 공통 질문
    # 채용 절차와 의미가 다르므로 process보다 우선 판별
    if any(k in h for k in APPLICATION_QUESTION_KEYWORDS):
        return "application_questions"

    # 7️⃣ 채용 절차
    # 인터뷰, 전형 단계 등
    if any(k in h for k in PROCESS_KEYWORDS):
        return "process"

    # 위 조건에 해당하지 않는 경우:
    # - 분류 가치가 낮은 섹션
    # - 추후 처리 가능한 영역
    return "others"
