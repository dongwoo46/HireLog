# src/infra/redis/stream_serializer.py

class RedisStreamSerializer:
    """
    Redis Stream Message Serializer

    책임:
    - metadata + payload → Redis Stream flat map 변환
    - Spring RedisStreamSerializer와 개념적 구조 일치

    출력 예:
    {
        "meta.type": "...",
        "meta.requestId": "...",
        "payload.canonicalText": "..."
    }
    """

    @staticmethod
    def serialize(
            metadata: dict[str, str],
            payload: dict[str, str],
    ) -> dict[str, str]:

        if not metadata:
            raise ValueError("metadata must not be empty")
        if not payload:
            raise ValueError("payload must not be empty")

        message: dict[str, str] = {}

        for k, v in metadata.items():
            message[f"meta.{k}"] = str(v)

        for k, v in payload.items():
            message[f"payload.{k}"] = str(v)

        return message
