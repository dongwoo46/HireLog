# src/inputs/jd_preprocess_input.py


from dataclasses import dataclass
from typing import Optional


@dataclass
class JdPreprocessInput:
    """
    JD 전처리 입력 DTO

    Spring Redis Stream 메시지 기준
    """

    # metadata
    request_id: str
    brand_name: str
    position_name: str
    source: str              # ✅ 추가 (TEXT / IMAGE / URL)

    created_at: int
    message_version: str

    # ---- payload ----
    text: Optional[str] = None
    images: Optional[list] = None
    url: Optional[str] = None          # ✅ 추가
