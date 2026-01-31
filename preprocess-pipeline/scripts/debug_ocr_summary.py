# scripts/debug_jd_preprocess_ocr.py

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
from preprocess.worker.redis.jd_preprocess_ocr_worker import JdPreprocessOcrWorker
from inputs.parse_jd_preprocess_message import parse_jd_preprocess_message
import json
from dataclasses import asdict

logger = logging.getLogger(__name__)


def main():
    redis_client = RedisClient()

    # ==================================================
    # 🔥 OCR 전용 Stream 소비
    # ==================================================
    consumer = RedisStreamConsumer(
        redis_client=redis_client,
        stream_key=JdStreamKeys.PREPROCESS_OCR_REQUEST,   # ✅ OCR 전용
        group="jd-ocr-group",
        consumer_name="jd-ocr-consumer-1",
    )

    worker = JdPreprocessOcrWorker()

    messages = consumer.read()

    for msg in messages:
        entry_id = msg.get("id")

        # ==================================================
        # 1️⃣ 메시지 → Input
        # ==================================================
        input = parse_jd_preprocess_message(msg)

        try:
            # ==================================================
            # 방어적 체크 (OCR 워커 안전망)
            # ==================================================
            if input.source != "IMAGE":
                raise ValueError(
                    f"OCR worker received non-IMAGE source: {input.source}"
                )

            # ==================================================
            # 2️⃣ OCR 전처리 실행
            # ==================================================
            output = worker.process(input)

            print("[DEBUG] JdPreprocessOutput (asdict):")
            print(json.dumps(asdict(output), ensure_ascii=False, indent=2))
            # ==================================================
            # 3️⃣ 성공 시 ACK
            # ==================================================
            consumer.ack(entry_id)

        except Exception as e:
            # Worker 내부에서 stack trace는 이미 남김
            logger.error(
                "[JD_OCR_PREPROCESS_ABORTED] requestId=%s entryId=%s errorType=%s errorMessage=%s",
                getattr(input, "request_id", None),
                entry_id,
                type(e).__name__,
                str(e),
            )
            # ACK ❌ → Pending 유지 (재처리)


if __name__ == "__main__":
    main()
