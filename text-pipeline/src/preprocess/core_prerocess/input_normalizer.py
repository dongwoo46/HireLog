import re
import unicodedata


# ==================================================
# 정규식 미리 컴파일 (성능 + 결정성)
# ==================================================

# 제어 문자 제거: U+0000 ~ U+001F, U+007F
# (개행은 별도 처리)
_CONTROL_CHARS_RE = re.compile(r"[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]")

# zero-width 문자
_ZERO_WIDTH_RE = re.compile(r"[\u200b\u200c\u200d]")

# 연속 공백 (스페이스만)
_MULTI_SPACE_RE = re.compile(r"[ ]{2,}")

# 개행 정규화
_NEWLINE_RE = re.compile(r"\r\n|\r")

# 문자 타입 경계 감지 (OCR / 웹 복사 대응)
_HANGUL_ENG_RE = re.compile(r"([가-힣])([A-Za-z])")
_ENG_HANGUL_RE = re.compile(r"([A-Za-z])([가-힣])")
_HANGUL_NUM_RE = re.compile(r"([가-힣])([0-9])")
_NUM_HANGUL_RE = re.compile(r"([0-9])([가-힣])")


def normalize_input_text(text: str) -> str:
    """
    JD Core Preprocessing - Input Normalization

    역할:
    - OCR / TEXT / URL 입력의 문자 레벨 차이 제거
    - 의미 / 구조 / 라인 판단은 절대 하지 않음
    - 이후 모든 파이프라인의 결정적 입력을 생성

    설계 원칙:
    - Preserve First (보존 우선)
    - Deterministic / Idempotent
    - 문자 레벨 정규화만 수행

    ❌ 하지 않는 것:
    - strip / trim
    - 라인 병합 또는 분해
    - 빈 줄 제거
    - 대소문자 변환
    - 의미 추론
    """
    if not text:
        return ""

    # 1️⃣ Unicode 정규화
    text = unicodedata.normalize("NFKC", text)

    # 2️⃣ 개행 표준화
    text = _NEWLINE_RE.sub("\n", text)

    # 3️⃣ BOM 제거
    text = text.replace("\ufeff", "")

    # 4️⃣ zero-width 제거
    text = _ZERO_WIDTH_RE.sub("", text)

    # 5️⃣ 제어 문자 제거
    text = _CONTROL_CHARS_RE.sub("", text)

    # 6️⃣ 탭 → 공백
    text = text.replace("\t", " ")

    # ⭐ 7️⃣ 문자 타입 경계 공백 삽입 (핵심)
    text = _HANGUL_ENG_RE.sub(r"\1 \2", text)
    text = _ENG_HANGUL_RE.sub(r"\1 \2", text)
    text = _HANGUL_NUM_RE.sub(r"\1 \2", text)
    text = _NUM_HANGUL_RE.sub(r"\1 \2", text)

    # 8️⃣ 연속 공백 축약
    text = _MULTI_SPACE_RE.sub(" ", text)

    return text

