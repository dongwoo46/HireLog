from inputs.jd_preprocess_input import JdPreprocessInput


class MessageParseError(ValueError):
    """메시지 파싱 오류"""
    pass


def parse_jd_preprocess_message(message: dict) -> JdPreprocessInput:
    """
    Redis Stream 메시지를 JD 전처리 입력 DTO로 변환

    책임:
    - 메시지 계약 검증
    - 필수 필드 검증

    Raises:
        MessageParseError: 필수 필드 누락 또는 형식 오류
    """
    if not message or "values" not in message:
        raise MessageParseError("Invalid message format: 'values' key is required")

    values = message["values"]

    if not isinstance(values, dict):
        raise MessageParseError("Invalid message format: 'values' must be a dict")

    # ---- metadata ----
    message_type = values.get("type")
    request_id = values.get("requestId")
    brand_name = values.get("brandName")
    position_name = values.get("positionName")
    created_at = values.get("createdAt")
    message_version = values.get("messageVersion")

    if message_type != "JD_PREPROCESS_REQUEST":
        raise MessageParseError(f"Unsupported message type: {message_type}")

    if not request_id:
        raise MessageParseError("Required field missing: requestId")

    if not brand_name:
        raise MessageParseError("Required field missing: brandName")

    if not position_name:
        raise MessageParseError("Required field missing: positionName")

    if not created_at:
        raise MessageParseError("Required field missing: createdAt")

    if not message_version:
        raise MessageParseError("Required field missing: messageVersion")

    # ---- payload ----
    source = values.get("payload.source")
    if source not in ("TEXT", "IMAGE", "URL"):
        raise ValueError(f"Invalid source: {source}")

    text = values.get("payload.text")
    
    # [Fix] Redis Stream message might contain 'payload.images' as a string or list.
    # User confirmed the key is 'payload.images'.
    raw_images = values.get("payload.images")
    images = []
    if raw_images:
        if isinstance(raw_images, str):
            # If it's a string, treat as single path
            images = [raw_images]
        elif isinstance(raw_images, list):
            images = raw_images

    url = values.get("payload.sourceUrl") or values.get("payload.url")

    if source == "TEXT" and not text:
        raise ValueError("payload.text is required when source=TEXT")

    if source == "IMAGE" and not images:
        raise ValueError("payload.images is required when source=IMAGE")

    if source == "URL" and not url:
        raise ValueError("payload.sourceUrl is required when source=URL")

    # createdAt 타입 변환 (int 또는 int로 변환 가능한 문자열)
    try:
        created_at_int = int(created_at)
    except (TypeError, ValueError) as e:
        raise MessageParseError(
            f"Invalid createdAt format: '{created_at}' is not convertible to int"
        ) from e

    return JdPreprocessInput(
        request_id=request_id,
        brand_name=brand_name,
        position_name=position_name,
        source=source,
        created_at=created_at_int,
        message_version=message_version,
        text=text,
        images=images,
        url=url,
    )
