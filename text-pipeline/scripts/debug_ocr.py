import sys
from pathlib import Path
from structure.header_grouping import extract_sections_by_header

"""
OCR + JD 파이프라인 디버그 실행 스크립트

이 스크립트는 무엇을 하는가?
--------------------------------------------------
- 로컬 환경에서
  1) OCR 파이프라인
  2) JD 해석 파이프라인
  을 순차적으로 실행해본다.

- 운영 코드가 아니라,
  ▶ 전체 데이터 흐름이 올바른지 확인하는 "조립 테스트용" 스크립트다.

- 이 파일에서는:
  ❌ 로직을 구현하지 않는다
  ✅ 이미 구현된 파이프라인을 호출해서 출력만 확인한다
"""

# --------------------------------------------------
# 프로젝트 경로 설정
# --------------------------------------------------

# 현재 파일 기준으로 프로젝트 루트 디렉토리 계산
ROOT_DIR = Path(__file__).resolve().parents[1]

# src 디렉토리를 Python import 경로에 추가
# → scripts/ 에서 실행해도 src/ 모듈들이 정상 import 되게 하기 위함
SRC_DIR = ROOT_DIR / "src"
if str(SRC_DIR) not in sys.path:
    sys.path.insert(0, str(SRC_DIR))


# --------------------------------------------------
# 파이프라인 import
# --------------------------------------------------

# OCR 전용 파이프라인
# - 이미지 → 텍스트(lines + rawText) 변환 담당
from inputs.ocr_input import process_ocr_input


def main():
    """
    디버그 실행 메인 함수

    실행 흐름:
    1) OCR 파이프라인 실행
    2) OCR 결과 출력 (사람 확인용)
    3) JD 파이프라인 실행
    4) JD 구조화 결과 출력
    """

    # --------------------------------------------------
    # 테스트할 이미지 경로
    # --------------------------------------------------
    image_path = "data/raw/sample2.png"

    # ==================================================
    # 1️⃣ OCR 파이프라인 실행
    # ==================================================
    # 이 단계의 책임:
    # - 이미지를 텍스트로 "복원"
    # - JD인지 아닌지는 여기서 판단하지 않음
    ocr_result = process_ocr_input(image_path)

    # --------------------------------------------------
    # OCR 결과 출력 (디버그용)
    # --------------------------------------------------
    print("OCR RESULT")
    print("==========")
    print(f"STATUS     : {ocr_result['status']}")
    print(f"CONFIDENCE : {ocr_result['confidence']:.2f}")

    # 사람이 읽는 용도의 원문 텍스트
    print("\nRAW TEXT\n--------")
    print(ocr_result["rawText"])

    # 2️⃣ Header 기반 JD 구조화
    sections = extract_sections_by_header(ocr_result["lines"])

    # JD 구조화 결과 출력
    print("\nJD STRUCTURED SECTIONS")
    print("======================")

    if not sections:
        print("(no sections detected)")
        return

    for header, lines in sections.items():
        print(f"\n[{header}]")

        if not lines:
            print("  (empty)")
            continue

        for text in lines:
            print(f"  - {text}")



# --------------------------------------------------
# 스크립트 직접 실행 시 진입점
# --------------------------------------------------
if __name__ == "__main__":
    main()
