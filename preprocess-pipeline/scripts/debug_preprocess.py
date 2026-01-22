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
import sys
from pathlib import Path

# ==================================================
# 프로젝트 루트 / src 경로 강제 등록
# ==================================================
ROOT_DIR = Path(__file__).resolve().parents[1]
SRC_DIR = ROOT_DIR / "src"

if str(SRC_DIR) not in sys.path:
    sys.path.insert(0, str(SRC_DIR))

from preprocess.core_preprocess.core_preprocessor import CorePreprocessor
from preprocess.structural_preprocess.structural_preprocessor import StructuralPreprocessor
from preprocess.semantic.semantic_preprocessor import apply_semantic_lite
from preprocess.semantic.section_filter import filter_irrelevant_sections
from preprocess.metadata_preprocess.metadata_preprocessor import MetadataPreprocessor


# ==================================================
# BASE JD INPUT (디버그 기준 입력)
# ==================================================

BASE_JD_TEXT = """
낭만아지트∙서울 서대문구∙신입∙인턴



[인턴] 프론트엔드 엔지니어

응답률
매우 높음

합격보상
지원자, 추천인 각 현금 50만원

포지션 상세
대화로 찾는 나에게 딱 맞는 공간, 나만의집

집을 고르는 순간에도 낭만이 필요합니다.
나만의집은 부동산에 특화된 AI를 통해 사람들이 자신에게 맞는 공간을 더 빠르고 안전하게 선택할 수 있도록 돕습니다.

낭만아지트는 AI 기반 부동산 서비스 ‘나만의집'을 운영하고 있습니다. 부동산 탐색 자동화와 맞춤형 매물 추천 솔루션을 통해 사용자의 주거 선택 경험을 혁신하고 있으며, 서비스 오픈 직후 초기창업패키지(딥테크) 선정, 시드 투자 유치, 팁스(TIPS) 선정 등을 통해 기술력과 시장성을 인정받았습니다.

나만의집 프론트엔드 팀은 AI와 사용자가 자연스럽게 대화하며 집을 찾을 수 있도록, 서비스의 핵심 사용자 경험을 설계하고 구현합니다.
AI 추천 로직과 대규모 부동산 데이터를 기반으로, 복잡한 정보를 직관적인 UI와 인터랙션으로 풀어내는 프론트엔드 구조를 개발합니다.

우리는 집을 단순한 매물이 아닌, 사람의 삶과 선택이 담긴 공간으로 바라봅니다. 이를 기술로 구현하기 위해 프론트엔드 팀은 사용자 경험과 웹 성능 최적화를 최우선으로 고민하며, AI와 서비스가 자연스럽게 연결되는 구조를 만들어갑니다.
주요업무
1) 나만의집 웹(Web) 프론트엔드 개발
· Next.js(App Router) 기반 서비스 화면 개발 및 운영
2) 나만의집 앱(App) 프론트엔드 개발
· React Native(WebView) 환경에서 동작하는 프론트엔드 화면 개발
· Web App 간 인터페이스 설계 (딥링크, 이벤트, 상태 전달 등)
3) 매물 탐색, 추천 결과, 대화 흐름 등 복잡한 정보를 직관적으로 표현
· 추천 흐름, 질문–응답, 후속 액션을 고려한 프론트엔드 로직 설계
4) API 연동 및 상태 관리 구조 설계
· 백엔드 RESTful API 연동 및 데이터 흐름 관리
5) 성능 최적화 및 사용자 경험 개선을 위한 지속적인 리팩토링
• 정형 데이터(매물 정보) 처리를 위한 DB설계
• AI 검색을 위한 Vector DB구축 및 최적화
5) 하이브리드 검색 엔진 고도화
• 정형 필터와 비정형 벡터 검색을 결합한 하이브리드 검색 로직 구현
자격요건
• 경력 무관(신입 지원 가능)
• HTML, CSS, JavaScript(ES6+)에 대한 기초 지식을 갖추신 분
• Next.js 기반 웹 서비스 개발 경험 또는 이에 준하는 SSR/CSR 구조에 대한 이해
• 웹 서비스의 기본 동작 원리(렌더링, 상태 변화 등)에 대한 이해를 갖추신 분
• Git을 활용한 소스 코드 버전 관리 및 협업이 가능하신 분
• 새로운 기술을 배우는 것을 두려워하지 않고, 주도적으로 문제를 해결하려는 분
• 인턴(수습기간) 3개월 평가 후 정규직 전환이 가능한 분 (졸업예정자 또는 기졸업자)
• 성공에 대한 열망을 가지고 도전을 즐길 수 있는 분
고용조건
• 정규직전환형
• 인턴 3개월
우대사항
• React 또는 Next.js 기반 개인 프로젝트 경험이 있으신 분
• API 연동을 통해 데이터를 화면에 표시해본 경험이 있으신 분
• Figma 등 디자인 시안을 기반으로 UI를 구현해본 경험
• 재사용 가능한 컴포넌트 설계 및 디자인 시스템 구축 경험
• AI 기반 서비스 또는 대화형 UI 개발 경험이 있으신 분
• 부동산 서비스 또는 프롭테크 산업에 대한 관심을 가진 분
혜택 및 복지
• 기업 내규
채용 전형
[전형안내]
• 자유로운 양식의 이력서를 보내주세요.
• 서류전형 ＞ 1차 면접전형(역량 및 문화적합성) ＞ 2차 면접전형(직무) ＞최종합격
• 해당 공고는 수시 채용으로 채용 완료 시 조기 마감될 수 있습니다.
• 제출 서류 내 기재사항이 허위로 판명될 경우 채용이 취소될 수 있습니다.
• 자유 양식의 이력서 및 경력 기술서는 PDF 파일로 변환 후 제출을 권장드립니다.

[근무조건]
• 채용 형태 : 3개월 수습기간 후 평가에 따라 정규직 전환
• 급여 : 면접 후 결정
• 근무지 : 서울시 서대문구 연세로 50 연세대학교 캠퍼스타운
• 근무시간 : 주 5일(월~금) 10:00 ~ 19:00
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
def debug_print_final_with_meta(sections, meta):
    print("\n" + "=" * 80)
    print("✅ FINAL JD PREPROCESS RESULT")
    print("=" * 80)

    # ==================================================
    # DOCUMENT META
    # ==================================================
    print("\n[DOCUMENT META]")

    period = meta.recruitment_period

    print(f"- Period Type : {period.period_type}")

    if period.open_date or period.close_date:
        print(f"- Open Date   : {period.open_date}")
        print(f"- Close Date  : {period.close_date}")

    if period.raw_texts:
        print("- Raw Texts:")
        for raw in period.raw_texts:
            print(f"  • {raw}")
    else:
        print("- Raw Texts   : (none)")

    print("\n" + "=" * 80)

    # ==================================================
    # SECTIONS
    # ==================================================
    for sec in sections:
        zone = sec.semantic_zone
        header = sec.header or "(no header)"

        print(f"\n[{zone}] {header}")

        for line in sec.lines:
            print(f"- {line}")

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
    # ⭐ 1.5️⃣ Metadata Preprocessing (lines 기준)
    metadata_preprocessor = MetadataPreprocessor()
    document_meta = metadata_preprocessor.process(core_lines)

    # 2️⃣ Structural Preprocessing
    structural = StructuralPreprocessor()
    sections = structural.process(core_lines)
    # debug_print_structural(sections)

    # 3️⃣ Semantic-lite
    sections = apply_semantic_lite(sections)
    sections = filter_irrelevant_sections(sections)  # ✅ 여기

    # debug_print_semantic(sections)

    # 4️⃣ Final output only
    debug_print_final_with_meta(sections, document_meta)


if __name__ == "__main__":
    main()