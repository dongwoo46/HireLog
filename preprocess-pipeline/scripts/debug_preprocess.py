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
import io
from pathlib import Path

# Windows 인코딩 문제 해결
if sys.platform == 'win32':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

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
[NAVER Cloud] 차세대 Cloud API 개발 (경력)
모집 부서NAVER Cloud 모집 분야Tech 모집 분야Backend 모집 경력경력 근로 조건정규모집 기간
2026.01.19 ~ 2026.02.06 (17:00)
Share Save
지원하기

﻿Banner (1).png


부서소개

저희 부서는 퍼블릭 클라우드 부문 국내 1위인 NAVER Cloud Platform 의 차세대 API 개발을 담당합니다.

네이버클라우드는 수많은 사용자가 이용하는 환경에서 안정적이고 확장 가능한 플랫폼 서비스를 제공하고 있습니다. 저희 팀은 사용자 친화적인 클라우드 서비스 API를 제공하기 위해 OpenAPI 스펙을 기반으로 API를 설계하고, 이를 바탕으로 SDK, Terraform, CLI 등을 자동 생성할 수 있는 개발 흐름(CI/CD 환경, 내부 시스템 연동)을 구축하고 있습니다.

Java / Go / Typescript / Python 기반의 SDK와 개발자 경험(DX)을 향상시키는 다양한 도구들도 함께 개발하고 있습니다.

담당업무

﻿• 차세대 클라우드 API를 위한 API 표준 연구 개발

﻿• API 개발 흐름 구축

﻿• SDK 및 개발자 도구(CLI, Terraform) 설계 및 개발

﻿• Internal API 플랫폼 서비스 개발

﻿• NCP API 개발자 생태계 구축

자격요건

﻿• 소프트웨어 개발 경력을 3년 이상 보유하신 분

• Java/Kotlin 및 Spring framework를 활용한 개발 경험을 보유하신 분

• 컨테이너 기반 환경(Kubernetes 등) 서비스 배포 및 운영 경험을 보유하신 분

• 현대적인 서비스 통신 방식(REST, gRPC 등)에 대한 이해를 바탕으로 확장성과 성능, 그리고 보안을 고려한 API 설계 및 개발 경험을 보유하신 분

우대사항

• 클라우드 환경에서 백엔드 서비스를 개발 및 운영한 경험을 보유하신 분

• AI 개발 도구를 활용해 개발 생산성 향상시킨 경험을 보유하신 분

• 오픈소스 기여 또는 오픈소스 기반 애플리케이션 개발 경험을 보유하신 분

• 사용자 중심의 API 설계 및 개발 경험, 혹은 전사 차원의 표준 컨벤션 수립 경험을 보유하신 분

• NCP, AWS 등의 개발자 도구(SDK, CLI, Terraform) 활용한 애플리케이션 개발 경험을 보유하신 분

• 다양한 프로그래밍 언어에 관심이 있으며 실제로 개발해 본 경험을 보유하신 분

전형절차 및 기타사항

[전형절차]

서류전형(기업문화적합도 검사 및 직무 테스트 포함) ▶ 1차 인터뷰 ▶ 레퍼런스체크 및 2차 인터뷰 ▶ 처우협의 ▶ 최종합격
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
    print("FINAL JD PREPROCESS RESULT")
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
            try:
                print(f"  - {raw}")
            except UnicodeEncodeError:
                print(f"  - {raw.encode('utf-8', errors='replace').decode('utf-8', errors='replace')}")
    else:
        print("- Raw Texts   : (none)")

    # 기술 스택 정보 출력
    skill_set = meta.skill_set
    

    if skill_set.skills:
        skills_str = ', '.join(skill_set.skills)
        print(f"\n- Skills ({len(skill_set.skills)}): {skills_str}")
    else:
        print("\n- Skills: (none)")

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