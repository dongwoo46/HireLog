# src/outputs/jd_preprocess_output.py

from dataclasses import dataclass

@dataclass
class JdPreprocessOutput:
    """
    JD 전처리 결과 Output DTO
    """

    # ==================================================
    # Message Meta
    # ==================================================
    type: str
    message_version: str
    created_at: int

    # ==================================================
    # Correlation / Context
    # ==================================================
    request_id: str
    brand_name: str
    position_name: str
    source: str

    # ==================================================
    # Canonical Result (🔥 핵심)
    # ==================================================
    canonical_map: dict[str, list[str]]

    # ==================================================
    # Recruitment Period
    # ==================================================
    recruitment_period_type: str|None = None
    recruitment_open_date:  str|None= None
    recruitment_close_date: str|None = None
