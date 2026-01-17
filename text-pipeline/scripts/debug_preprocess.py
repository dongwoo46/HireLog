"""
JD Preprocessing Debug Runner (BASE INPUT MODE)

- 기준 JD 텍스트를 코드에 고정
- Core → Structural → Semantic-lite 전 과정을 실행
- 전처리 로직 변경 시 결과 비교용

목적:
- 헤더 인식이 잘 되는지
- 섹션 분리가 제대로 되는지
- 리스트 / semantic zone이 의도대로 나뉘는지
"""

from preprocess.core_prerocess.core_preprocessor import CorePreprocessor
from preprocess.structural_preprocess.structural_preprocessor import StructuralPreprocessor
from preprocess.semantic.semantic_preprocessor import apply_semantic_lite
from preprocess.semantic.section_filter import filter_irrelevant_sections


# ==================================================
# BASE JD INPUT (디버그 기준 입력)
# ==================================================

BASE_JD_TEXT = """
와드(캐치테이블)∙경기 성남시∙경력 2-7년



검색 Back_End Developer

응답률
평균이상

합격보상
지원자, 추천인 각 현금 50만원

포지션 상세
“즐거운 미식생활의 시작, 캐치테이블”

누군가에는 설레는 약속이고, 또 다른 이에게는 중요한 비즈니스의 시작입니다.
그 중요한 순간들이 더 즐겁고 간편해지도록, 캐치테이블은 예약, 웨이팅, 포스, 픽업, 결제까지 아우르는
'역사상 최초의 요식업 슈퍼플랫폼'을 만들어 나가고 있습니다.

"국내 1위 레스토랑 예약 서비스"
실시간 웨이팅 시스템 ‘캐치테이블 웨이팅’
B2C & B2B 올인원 통합 솔루션
예약금 0원 ‘캐치페이’, 티맵 연동, AI 레스토랑 추천, 글로벌 예약 (일본)
요식업 슈퍼플랫폼으로 진화한다. MAU 500만 달성, 천만 고객 돌파까지

앞으로 우리는 단일 기능의 서비스를 넘어
요식업의 모든 접점을 하나로 통합하는 '슈퍼플랫폼'으로 요식업 산업의 새로운 기준을 만들어 나갈 것입니다.
그 변화의 한가운데에서 함께할 동료를 기다립니다.

[유의사항]
• 제출 자료와 채용 프로세스 전반에서 허위사실 및 결격사유가 발견될 경우 채용이 취소될 수 있습니다.
• 모든 공고는 수시채용으로, 합격자가 발생할 경우 조기 마감될 수 있습니다.
• 채용 포지션에 따라 프로세스가 변경 또는 추가될 수 있습니다.
주요업무
• 캐치테이블 내 검색/탐색 기능 제공을 위한 데이터 수집, 색인, 분석 시스템을 설계,구현, 운영하며 성능과 구조를 개선합니다.
• 검색/탐색 백엔드 서비스/시스템과 관련된 요구사항들을 정제하여 시스템을 설계, 구현하고 문제를 해결합니다.
• 관련된 관리자 도구를 기획 및 구현하고 운영합니다.
• 검색/탐색 서비스를 안정적으로 운영할 수 있는 제반 환경을 구축/도입합니다.
자격요건
• 고객향 서비스의 백엔드 엔지니어로 최소 경력 2년 이상
• 하나 이상의 서비스를 최소 2년 이상 운영하고 지속 개선/발전시켜 본 경험이 있으신 분
• 순간적인 트래픽 및 대량의 트래픽을 견딜 수 있는 읽기 위주의 데이터 모델링 및 서비스 설계/구현/운영/개선 경험이 있으신 분
• java 또는 kotlin 언어에 능통하신 분
• Information Retrieval에 대한 기초적인 이해/지식
우대사항
• elasticsearch 클러스터 구축 및 운영 경험
• elasticsearch 기반 키워드 검색 서비스 개발/운영 경험
• NLP에 대한 기초적인 이해/지식
• 기초적인 데이터 분석 및 통계 관련지식
• 데이터 엔니지어링 관련 기초적인 지식 및 경험

[이런 성향의 분이면 더 좋겠어요]
• 더 나은 구조 / 코드 / 문제 해결 방안을 추구하는데 진심이신 분
• 운영이 쉽고, 쉽게 확장 가능한 결과물을 만들기 위해 끊임없이 고민하시는 분
• 건설적인 비판과 토론에 열려 있는 분
• 다양한 기술 수준의 문제에 대해 적정한 기술 수준의 결과물을 추구하는 분
• 계획보다 현실적이고 실용적인 결과물/해결책을 제시하고, 바로 행동으로 실천 하시는 분
• 팀 플레이어

[기술스택]
•Java, Kotlin, Python, Spring, Gradle, Redis, Kafka, Airflow, Docker
•인프라 : AWS, GCP, Kubernetes
•업무 도구 : Git, Slack, Jira, Confluence, Intellij, Claude Code
•테스트, 빌드, 배포 : ArgoCD, GitHub Actions
혜택 및 복지
[혜택 및 복지]
I 당신의 성장이 곧 캐치테이블의 성장 I
• 독서모임을 위한 도서 및 직무 관련 도서, 세미나, 컨퍼런스 등 교육비 지원
• 미식생활을 즐기는 자가 미식문화를 선도할 수 있음에 연간 캐치포인트 100만원 지원
• 장기근속 포상 휴가 및 휴가비 지원 (3년: 5일, 100만원 / 5년, 7년: 10일, 300만원 / 10년: 20일, 500만원)

l 일에만 집중할 수 있도록 I
• 추가근무수당(1시간당 15,000원) 및 택시비(오후 11시 퇴근 경우) 지원
• 점심 식사, 추가 근무의 경우 저녁 식사 지원
• 다양한 음료와 간식, 커피 무제한 제공
• 최고사양 IT 자산 지급
• 넓은 오피스 및 편한 인프라 환경을 갖춘 판교 유스페이스 입주사
• 정밀 건강 검진 2년에 1회 제공
• 마음 건강 검진, 연간 4회 심리상담 지원
• 복지를 편하게 이용하기 위한 개별 법인카드 지급

I 소통&소통 I
• 매월 랜덤으로 타 팀 구성원들과 짝지어 점심 먹는 ‘Leaders Lunch Day’
• 회사의 비전, 팀의 목표, 향후 계획 등을 CEO가 직접 공유하는 ‘Monthly Growth Meeting’
• 개인의 성장에 집중하여 리더와 함께 이야기 나누는 'Monthly Feedback Meeting'
• 신규 입사자 새로운 시작을 함께 도와주는 'Categorie'
• 소통의 시너지를 위해 타 팀 구성원들과 함께하는 문화의 날 'Leaders Culture Day'

I 기념일도 함께하는 캐치테이블 I
• 생일 케이크, 생일 반차 및 상품권 지급
• 명절(설날, 추석) 반차 및 상품권 지급
채용 전형
서류 접수 ＞ 코딩 테스트 ＞ 직무 적합성 검사 ＞ 1차 인터뷰 ＞ 2차 인터뷰 ＞ 레퍼런스 체크 ＞ 처우 협의 ＞ 최종 합격 및 입사
※ 직무 적합성 검사는 채용의 참고 자료로만 활용되며, 합격 여부를 결정짓는 기준으로 사용되지 않습니다.
※ 1차 인터뷰 요청 시 실제 진행하셨던 주요 프로젝트 1건에 대한 추가 자료를 요청드릴 수 있습니다.
태그

커피·스낵바

휴가비

자기계발지원

건강검진지원

연봉상위11~20%

51~300명

설립4~9년

인원 급성장

워크샵

누적투자100억이상
마감일
상시채용
""".strip()


# ==================================================
# Debug Print Helpers
# ==================================================

# ==================================================
# Debug Print Helpers
# ==================================================

def print_block(title: str):
    """
    큰 단계 전환용 구분선 출력
    """
    print("\n" + "=" * 80)
    print(title)
    print("=" * 80)


def debug_print_core(lines: list[str]):
    print_block("1️⃣ CORE PREPROCESS RESULT (List[str])")

    for idx, line in enumerate(lines, 1):
        print(f"{idx:03d}: {line}")

    print("=" * 80)


def debug_print_structural(sections):
    print_block("2️⃣ STRUCTURAL PREPROCESS RESULT (Sections)")

    for i, sec in enumerate(sections, 1):
        print(f"\n--- Section {i} ---")
        print(f"Header : {sec.header}")

        print("Lines:")
        for line in sec.lines:
            print(f"  {line}")

        print("Lists:")
        for lst in sec.lists:
            print(f"  - {lst}")

    print("=" * 80)


def debug_print_semantic(sections):
    print_block("3️⃣ SEMANTIC-LITE RESULT (semantic_zone)")

    for i, sec in enumerate(sections, 1):
        print(f"\n--- Section {i} ---")
        print(f"Header        : {sec.header}")
        print(f"Semantic Zone : {sec.semantic_zone}")

        print("Lines:")
        for line in sec.lines:
            print(f"  {line}")

        print("Lists:")
        for lst in sec.lists:
            print(f"  - {lst}")

    print("=" * 80)

def debug_print_final(sections):
    """
    최종 JD 전처리 결과 출력

    - semantic_zone 기준으로 섹션 요약
    - downstream 처리 대상 형태에 가깝게 출력
    """

    print("\n" + "=" * 80)
    print("✅ FINAL JD PREPROCESS RESULT")
    print("=" * 80)

    for sec in sections:
        zone = sec.semantic_zone
        header = sec.header or "(no header)"

        print(f"\n[{zone}] {header}")

        # 일반 라인 출력
        for line in sec.lines:
            print(f"- {line}")

        # 리스트 항목 출력
        for lst in sec.lists:
            for item in lst:
                print(f"- {item}")

    print("\n" + "=" * 80)


# ==================================================
# Main
# ==================================================

def main():
    # BASE JD를 바로 사용 (RAW 출력 없음)
    raw_text = BASE_JD_TEXT

    # 1️⃣ Core Preprocessing
    core = CorePreprocessor()
    core_lines = core.process(raw_text)
    # debug_print_core(core_lines)

    # 2️⃣ Structural Preprocessing
    structural = StructuralPreprocessor()
    sections = structural.process(core_lines)
    # debug_print_structural(sections)

    # 3️⃣ Semantic-lite
    sections = apply_semantic_lite(sections)
    sections = filter_irrelevant_sections(sections)  # ✅ 여기

    # debug_print_semantic(semantic_sections)

    # 4️⃣ Final output only
    debug_print_final(sections)


if __name__ == "__main__":
    main()