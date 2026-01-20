from preprocess.core_preprocess.input_normalizer import normalize_input_text
from preprocess.core_preprocess.noise_filter import remove_ui_noise
from preprocess.core_preprocess.line_segmenter import segment_lines
from preprocess.core_preprocess.bullet_normalizer import normalize_bullets
from preprocess.core_preprocess.text_damage_guard import guard_text_damage
from common.noise.loader import load_noise_keywords


class CorePreprocessor:
    """
    JD Core Preprocessing Pipeline

    역할:
    - 입력 형태(OCR / TEXT / URL) 차이를 제거
    - JD 원문을 라인 문서 계약으로 변환
    - 이후 모든 레이어의 결정적 입력 제공
    """

    def __init__(self):
        self._ui_noise_patterns = load_noise_keywords()

    def process(self, raw_text: str) -> list[str]:
        """
        Core Preprocessing 단일 진입점

        반환:
        - List[str] (JD Line Document)
        """

        # 1️⃣ Input Normalization
        text = normalize_input_text(raw_text)

        # 2️⃣ UI / System Noise Removal
        text = remove_ui_noise(text, self._ui_noise_patterns)

        # 3️⃣ Line Segmentation
        lines = segment_lines(text)

        # 4️⃣ Bullet / List Normalization
        lines = normalize_bullets(lines)

        # 5️⃣ Text Damage Guard
        lines = guard_text_damage(lines)

        return lines
