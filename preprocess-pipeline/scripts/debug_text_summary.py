# scripts/debug_jd_preprocess_text.py

import sys
import os
import logging

# ==================================================
# 개발 환경에서 src 경로를 PYTHONPATH에 강제로 추가
# - 실제 배포 환경에서는 필요 없음
# ==================================================
sys.path.append(
    os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "src"))
)

from infra.redis.redis_client import RedisClient
from infra.redis.stream_consumer import RedisStreamConsumer
from infra.redis.stream_keys import JdStreamKeys
from preprocess.worker.redis.jd_preprocess_text_worker import JdPreprocessTextWorker
from inputs.parse_jd_preprocess_message import parse_jd_preprocess_message

logger = logging.getLogger(__name__)
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
)


def main():
    # ==================================================
    # Redis Client 초기화
    # ==================================================
    redis_client = RedisClient()

    # ==================================================
    # TEXT 전처리 요청 전용 Stream Consumer
    #
    # 역할:
    # - TEXT 기반 JD 전처리 요청만 소비
    # - OCR 요청은 절대 처리하지 않음
    # ==================================================
    consumer = RedisStreamConsumer(
        redis_client=redis_client,
        stream_key=JdStreamKeys.PREPROCESS_TEXT_REQUEST,
        group="jd-text-group",
        consumer_name="jd-text-consumer-1",
    )

    # ==================================================
    # TEXT 전처리 Worker
    # - 내부에서 Redis publish까지 수행
    # ==================================================
    worker = JdPreprocessTextWorker()

    # ==================================================
    # Stream 메시지 읽기
    # ==================================================
    messages = consumer.read()

    for msg in messages:
        entry_id = msg.get("id")

        # ==================================================
        # 1️⃣ Redis 메시지 → Input DTO 변환
        # ==================================================
        input = parse_jd_preprocess_message(msg)

        try:
            # ==================================================
            # 방어적 검증
            #
            # TEXT 워커는 TEXT 요청만 처리해야 함
            # ==================================================
            if input.source != "TEXT":
                raise ValueError(
                    f"TEXT worker received non-TEXT source: {input.source}"
                )

            # ==================================================
            # 2️⃣ TEXT 전처리 실행
            #
            # 주의:
            # - worker.process 내부에서
            #   - canonical_map 생성
            #   - document_meta 계산
            #   - Redis Stream publish 까지 완료됨
            #
            # 이 스크립트에서는
            # ❌ 결과를 다시 가공하거나 꺼내 쓰지 않는다
            # ==================================================
            output = worker.process(input)

            print(output)

            # ==================================================
            # 3️⃣ 정상 처리 시 ACK
            #
            # - Redis Stream Pending 제거
            # ==================================================
            consumer.ack(entry_id)

        except Exception as e:
            # ==================================================
            # 예외 발생 시
            # - ACK ❌
            # - Pending 유지 → 재처리 가능
            #
            # Worker 내부에서 stack trace는 이미 기록됨
            # ==================================================
            logger.error(
                "[JD_TEXT_PREPROCESS_ABORTED] requestId=%s entryId=%s errorType=%s errorMessage=%s",
                getattr(input, "request_id", None),
                entry_id,
                type(e).__name__,
                str(e),
            )


if __name__ == "__main__":
    main()
