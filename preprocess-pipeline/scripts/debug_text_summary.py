# scripts/debug_jd_preprocess_text.py

import sys
import os
import logging

# ==================================================
# src 경로를 PYTHONPATH에 추가 (개발용)
# ==================================================
sys.path.append(
    os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "src"))
)

from infra.redis.redis_client import RedisClient
from infra.redis.stream_consumer import RedisStreamConsumer
from infra.redis.stream_keys import JdStreamKeys
from preprocess.worker.jd_preprocess_text_worker import JdPreprocessTextWorker
from inputs.parse_jd_preprocess_message import parse_jd_preprocess_message

logger = logging.getLogger(__name__)
logging.basicConfig(
    level=logging.INFO,   # 🔥 INFO 이상 전부 출력
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
)

def main():
    redis_client = RedisClient()

    # ==================================================
    # 🟢 TEXT 전용 Stream 소비
    # ==================================================
    consumer = RedisStreamConsumer(
        redis_client=redis_client,
        stream_key=JdStreamKeys.PREPROCESS_TEXT_REQUEST,   # ✅ TEXT 전용
        group="jd-text-group",
        consumer_name="jd-text-consumer-1",
    )

    worker = JdPreprocessTextWorker()

    messages = consumer.read()

    for msg in messages:
        entry_id = msg.get("id")

        # ==================================================
        # 1️⃣ 메시지 → Input
        # ==================================================
        input = parse_jd_preprocess_message(msg)

        try:
            # ==================================================
            # 방어적 체크 (TEXT 워커 안전망)
            # ==================================================
            if input.source != "TEXT":
                raise ValueError(f"TEXT worker received non-TEXT source: {input.source}")

            # ==================================================
            # 2️⃣ TEXT 전처리 실행
            # ==================================================
            output = worker.process(input)

            canonical_text = output.canonical_text

            # ==================================================
            # 3️⃣ 성공 시 ACK
            # ==================================================
            consumer.ack(entry_id)

        except Exception as e:
            # Worker 내부에서 stack trace는 이미 남김
            logger.error(
                "[JD_TEXT_PREPROCESS_ABORTED] requestId=%s entryId=%s errorType=%s errorMessage=%s",
                getattr(input, "request_id", None),
                entry_id,
                type(e).__name__,
                str(e),
            )
            # ACK ❌ → Pending 유지 (재처리)


if __name__ == "__main__":
    main()
