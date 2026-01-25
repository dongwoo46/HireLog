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
[네이버웹툰] ML 플랫폼/추천시스템 개발 (경력)
모집 부서NAVER WEBTOON 모집 분야Tech 모집 분야AI/ML 모집 경력경력 근로 조건정규모집 기간
2026.01.15 ~ 2026.02.01 (23:59)
Share Save
지원하기
조직 소개

네이버웹툰, 시리즈, WEBTOON, LINE MANGA 등에 제공하는

추천 시스템 및 ML 플랫폼/서비스에 대해 연구·개발·운영합니다.

전 세계 1위 스토리테크 플랫폼에 걸맞은 대용량 데이터·트래픽·GPU 인프라를

효과적으로 활용하여 글로벌 서비스를 만들고 있습니다. 빠르게 높아지는 ML 수요에 대응하기 위해

MLOps 기반 플랫폼을 자체 구축하며, 전세계에 실시간 추천, CRM 서비스를 제공하고 있습니다. 


열정적이고 협업에 열려 있고, 새로움을 적용하는데 두려움이 없는 끊임없이 성장에 대해 고민하는 동료들이 있습니다.
최고 수준의 개발자들과 함께 No.1 Story Tech Platform에 ML Service를 통해 기여하는 경험을 하실 분을 기다립니다.

담당 업무

- ML 플랫폼을 위한 아키텍처 설계 및 고도화
- 추천 서비스 및 ML 플랫폼/서비스 백엔드 개발 및 운영

- 오픈소스·신규 기술 적극 도입 및 내재화 주도

필요 역량

- 3년 이상의 백엔드·플랫폼 개발 경험이 있으신 분

- 대용량 서비스 설계·구현·운영을 경험하신 분

- 시스템 성능 분석·최적화 능력이 뛰어나신 분

- 빠른 기술 학습 및 오픈소스·클라우드 기술 도입·주도 경험이 있으신 분

- Java·Python·C/C++ 등 하나 이상의 언어에 능숙하신 분

- AI/ML 경험이 있거나 없더라도 관심이 있으신 분

- 협업·커뮤니케이션 스킬이 뛰어나며 코드리뷰·문서화·지식 공유를 지향하시는 분

우대 사항

- 이벤트 기반 아키텍처를 설계·운영해보신 분

- Redis·MongoDB·Cassandra·HBase 등 NoSQL로 대용량 데이터 서비스를 운영해보신 분

- Airflow·Kafka 기반 데이터 파이프라인을 구축·운영해보신 분

- Spark·Flink 등 분산 처리 엔진으로 스트리밍·배치 파이프라인을 구현해보신 분

- PyTorch·TensorFlow 등 딥러닝 프레임워크를 실무에서 사용해보신 분 (리서처 코드 리뷰 경험 포함)

- LLM 실험 플랫폼·프롬프트 엔지니어링을 구축·운영해보신 분

- 10명 이상이 사용하는 코드베이스에서 중·대규모 협업을 경험해보신 분

- 자신만의 코딩 철학이 있으나 팀 컨벤션·협업 방식에 유연하게 적응 가능하신 분

- 일본 출장 가능하시거나 관심이 있으신 분

- 일본어 커뮤니케이션이 가능하신 분 (일본어 가능하신 분의 경우 실무 면접 시 간단한 어학 테스트가 있을 수 있습니다)

전형 절차 및 안내 사항

﻿- 전형절차: 지원서 리뷰 ▶ 프리 인터뷰 ▶ 실무 인터뷰(*코딩 테스트 포함) ▶ Culture-Fit 인터뷰(*기업문화 적합도 검사 포함)

* 코딩 테스트 시에는 AI Tool을 활용한 코딩 테스트도 진행되며, 관련한 상세 안내는 실무 인터뷰 진행 시 드릴 예정입니다.

- 고용형태: 정규직

- 근무 장소: 정자동 그린팩토리



* 전형 절차는 일정 및 상황에 따라 변경될 수 있습니다.

* 본 공고는 인재 선발 완료 시 조기 마감될 수 있으며, 필요 시 모집 기간이 연장될 수 있습니다.

* 지원서 내용 중 허위사실이 있는 경우에는 합격이 취소될 수 있습니다.

* 국가유공자 및 장애인 등 취업보호대상자는 관계법령에 따라 우대합니다.

* 문의사항은 "네이버웹툰 채용 홈페이지 > 1:1 문의"로 접수해주시기 바랍니다.
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