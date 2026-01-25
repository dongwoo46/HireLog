from inputs.jd_preprocess_input import JdPreprocessInput


def parse_jd_preprocess_message(message: dict) -> JdPreprocessInput:
    """
    Redis Stream 메시지를 JD 전처리 입력 DTO로 변환

    책임:
    - 메시지 계약 검증
    - 필수 필드 검증
    """
    values = message["values"]

    # ---- metadata ----
    message_type = values.get("type")
    request_id = values.get("requestId")
    brand_name = values.get("brandName")
    position_name = values.get("positionName")
    created_at = values.get("createdAt")
    message_version = values.get("messageVersion")

    if message_type != "JD_PREPROCESS_REQUEST":
        raise ValueError(f"Unsupported message type: {message_type}")

    if not request_id:
        raise ValueError("requestId is required")

    if not brand_name or not position_name:
        raise ValueError("brandName and positionName are required")

    if not created_at or not message_version:
        raise ValueError("createdAt and messageVersion are required")

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

    return JdPreprocessInput(
        request_id=request_id,
        brand_name=brand_name,
        position_name=position_name,
        source=source,
        created_at=int(created_at),
        message_version=message_version,
        text=text,
        images=images,
        url=url,
    )
