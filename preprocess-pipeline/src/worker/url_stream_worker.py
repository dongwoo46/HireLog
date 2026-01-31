# src/worker/url_stream_worker.py

import logging
from typing import Any, Dict

from worker.base_worker import BaseWorker
from infra.redis.stream_consumer import RedisStreamConsumer
from inputs.parse_jd_preprocess_message import parse_jd_preprocess_message, MessageParseError
from preprocess.worker.redis.jd_preprocess_url_worker import JdPreprocessUrlWorker
from infra.config.redis_config import WorkerConfig

logger = logging.getLogger(__name__)


class UrlStreamWorker(BaseWorker):
    """
    URL 소스 전용 Stream Worker

    책임:
    - jd:preprocess:url:request:stream 소비
    - 메시지 파싱 → JdPreprocessUrlWorker 호출
    - 성공 시 ACK, 실패 시 pending 유지
    """

    def __init__(
        self,
        consumer: RedisStreamConsumer,
        config: WorkerConfig,
    ):
        super().__init__(
            consumer=consumer,
            config=config,
            worker_name="URL_WORKER",
        )
        self.jd_worker = JdPreprocessUrlWorker()

    def process(self, message: Dict[str, Any]) -> bool:
        """
        URL 메시지 처리

        Returns:
            True: 처리 성공 (ACK 대상)
            False: 처리 실패 (pending 유지)
        """
        message_id = message.get("id", "unknown")

        try:
            jd_input = parse_jd_preprocess_message(message)

            logger.info(
                "[URL_WORKER] Processing | id=%s requestId=%s brand=%s url=%s",
                message_id,
                jd_input.request_id,
                jd_input.brand_name,
                jd_input.url,
            )

            self.jd_worker.process(jd_input)

            logger.info(
                "[URL_WORKER] Success | id=%s requestId=%s",
                message_id, jd_input.request_id
            )

            return True

        except MessageParseError as e:
            logger.error(
                "[URL_WORKER] Message parse error | id=%s error=%s",
                message_id, str(e)
            )
            return True  # 파싱 오류는 재시도해도 실패하므로 ACK

        except Exception as e:
            logger.error(
                "[URL_WORKER] Processing failed | id=%s error=%s",
                message_id, str(e),
                exc_info=True
            )
            return False  # 처리 실패는 pending 유지
