# infra/redis/serializers/jd_preprocess_serializer.py

import json
from datetime import datetime
from outputs.jd_preprocess_output import JdPreprocessOutput


def _normalize_date(date_str: str) -> str:
    """
    yyyy.MM.dd → yyyy-MM-dd (ISO-8601)
    """
    return datetime.strptime(date_str, "%Y.%m.%d").date().isoformat()


def serialize_preprocess_output(output: JdPreprocessOutput) -> dict[str, str]:
    payload: dict[str, str] = {}

    # =========================
    # Meta
    # =========================
    payload["meta.type"] = output.type
    payload["meta.messageVersion"] = output.message_version
    payload["meta.createdAt"] = str(output.created_at)

    payload["meta.requestId"] = output.request_id
    payload["meta.brandName"] = output.brand_name
    payload["meta.positionName"] = output.position_name

    # =========================
    # Payload (🔥 계약 핵심)
    # =========================
    payload["payload.source"] = output.source

    # 🔥 항상 존재해야 한다
    payload["payload.canonicalMap"] = json.dumps(output.canonical_map)

    if output.recruitment_period_type:
        payload["payload.recruitmentPeriodType"] = output.recruitment_period_type

    if output.recruitment_open_date:
        payload["payload.recruitmentOpenDate"] = _normalize_date(
            output.recruitment_open_date
        )

    if output.recruitment_close_date:
        payload["payload.recruitmentCloseDate"] = _normalize_date(
            output.recruitment_close_date
        )

    if output.skills:
        payload["payload.skills"] = json.dumps(output.skills)

    return payload
