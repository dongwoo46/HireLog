# scripts/debug_url_summary.py

import sys
import os
import logging
import time

# ==================================================
# 개발 환경에서 src 경로를 PYTHONPATH에 강제로 추가
# ==================================================
sys.path.append(
    os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "src"))
)

from infra.redis.redis_client import RedisClient
from infra.redis.stream_consumer import RedisStreamConsumer
from infra.redis.stream_keys import JdStreamKeys
from preprocess.worker.jd_preprocess_url_worker import JdPreprocessUrlWorker
from inputs.parse_jd_preprocess_message import parse_jd_preprocess_message
from inputs.jd_preprocess_input import JdPreprocessInput

logger = logging.getLogger(__name__)
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
)


def main():

    redis_client = RedisClient()
    
    # 1. Worker 초기화
    worker = JdPreprocessUrlWorker()
    
    # URL 전처리 요청 전용 Stream Consumer
    # 역할:
    # - TEXT 기반 JD 전처리 요청만 소비
    # - OCR 요청은 절대 처리하지 않음
    consumer = RedisStreamConsumer(
        redis_client=redis_client,
        stream_key=JdStreamKeys.PREPROCESS_URL_REQUEST,
        group="jd-url-group",
        consumer_name="jd-url-consumer-1"
    )

    worker = JdPreprocessUrlWorker()

    
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
            # URL 워커는 URL 요청만 처리해야 함
            # ==================================================
            if input.source != "URL":
                raise ValueError(
                    f"URL worker received non-URL source: {input.source}"
                )

            if not input.url:
                raise ValueError("URL worker received empty url")

            # ==================================================
            # 2️⃣ URL 전처리 실행
            #
            # 주의:
            # - worker.process 내부에서
            #   - URL fetch
            #   - TEXT 추출
            #   - canonical_map 생성
            #   - Redis Stream publish 까지 완료됨
            #
            # 이 스크립트에서는
            # ❌ 결과를 다시 가공하거나 저장하지 않는다
            # ==================================================
            output = worker.process(input)

            

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
                "[JD_URL_PREPROCESS_ABORTED] requestId=%s entryId=%s errorType=%s errorMessage=%s",
                getattr(input, "request_id", None),
                entry_id,
                type(e).__name__,
                str(e),
            )

if __name__ == "__main__":
    main()
