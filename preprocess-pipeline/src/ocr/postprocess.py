"""
JD 도메인 후처리 모듈

책임:
- OCR + normalize_lines를 통과한 라인을 입력으로 받아
- JD 의미를 보존하면서 최소한의 정제 수행
- 이 단계에서는 구조화/요약을 하지 않고
  '정제된 JD 원문'만 생성한다
"""

from common.noise.loader import load_noise_keywords
from ocr.filter_noise import filter_ocr_noise_lines
from normalize.token_normalizer import normalize_token
from common.vocab.loader import load_jd_vocab
from ocr.garbled_korean import is_garbled_korean
from common.section.loader import load_jd_meta_keywords
from common.section.loader import load_header_keywords

def is_korean_sentence(text: str) -> bool:
    """
    한국어 설명 문장인지 판단

    목적:
    - 조사/어미가 포함된 자연어 설명 문장은
      토큰 단위로 분해하면 의미가 깨짐
    - 이런 라인은 normalize_token을 적용하지 않고 그대로 보호
    """
    if not text:
        return False

    # 한글이 일정 개수 이상이면 설명 문장으로 간주
    return sum(1 for c in text if '가' <= c <= '힣') >= 3


def postprocess_ocr_lines(lines: list[dict]) -> list[dict]:
    """
    JD 도메인 후처리 파이프라인

    입력:
    - normalize_lines를 통과한 OCR 라인 리스트

    출력:
    - 의미가 보존된 JD 라인 리스트
    - (섹션 구조화, 요약 단계는 여기서 수행하지 않음)
    """

    # JD 무관 노이즈 키워드 (apply, privacy 등)
    noise_keywords = load_noise_keywords()

    # JD 기술 어휘 (java, spring, kafka 등)
    vocab = load_jd_vocab()

    # JD 메타 키워드 (전형절차, 인터뷰)
    meta_keywords = load_jd_meta_keywords()

    # 헤더 키워드 로딩
    header_keywords = load_header_keywords()   # ✅ 한 번만 로드

    # JD와 무관한 라인 제거
    # (푸터, 전형절차 안내 등 명백한 노이즈만 제거)
    filtered = filter_ocr_noise_lines(lines, noise_keywords)
    processed: list[dict] = []

    for line in filtered:
        text = line.get("text", "")
        section = line.get("section")
        # print(f"\n--- [LINE START] ---")
        # print(f"text = '{text}'")

        text_lower = text.lower()

        # 0️⃣ 헤더 키워드면 무조건 보존
        if text_lower in header_keywords:
            # print("✅ KEEP: header keyword")
            processed.append(line)
            continue

        # 1️⃣ OCR 깨진 한글 제거
        if is_garbled_korean(text):
            # print("❌ DROP: is_garbled_korean")
            continue

        # 2️⃣ JD 메타 정보 보호 (전형절차, 고용형태 등)
        if any(k in text_lower for k in meta_keywords):
            # print("✅ KEEP: meta keyword")
            processed.append(line)
            continue

        # 3️⃣ 한국어 설명 문장 보호
        if is_korean_sentence(text):
            processed.append(line)
            continue

        # 3️⃣ 기술/영문 토큰만 정규화
        tokens = text.split()
        # print(f"tokens = {tokens}")
        new_tokens: list[str] = []

        for token in tokens:
            normalized = normalize_token(
                token=token,
                section=section,
                vocab=vocab
            )

            # garbage 토큰은 제거
            if normalized is None:
                # print(f"  - DROP TOKEN '{token}'")
                continue

            new_tokens.append(normalized)

        # 4️⃣ 토큰 결과가 비어도 라인 전체는 유지
        # (의미 있는 라인이 사라지는 것을 방지)
        if not new_tokens:
            # print("⚠️ KEEP: empty tokens, keeping original")
            processed.append(line)
            continue
        
        new_text = " ".join(new_tokens)

        # 5️⃣ 정규화된 토큰으로 텍스트 교체
        processed.append({
            **line,
            "text": new_text
        })

    # 이 단계에서는 구조화(build_sections) 하지 않음
    return processed
