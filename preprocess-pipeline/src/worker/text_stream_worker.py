# src/worker/text_stream_worker.py

import logging
from typing import Any, Dict

from worker.base_worker import BaseWorker
from infra.redis.stream_consumer import RedisStreamConsumer
from inputs.parse_jd_preprocess_message import parse_jd_preprocess_message, MessageParseError
from preprocess.worker.jd_preprocess_text_worker import JdPreprocessTextWorker
from config import WorkerConfig

logger = logging.getLogger(__name__)


class TextStreamWorker(BaseWorker):
    """
    TEXT 소스 전용 Stream Worker

    책임:
    - jd:preprocess:text:request:stream 소비
    - 메시지 파싱 → JdPreprocessTextWorker 호출
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
            worker_name="TEXT_WORKER",
        )
        self.jd_worker = JdPreprocessTextWorker()

    def process(self, message: Dict[str, Any]) -> bool:
        """
        TEXT 메시지 처리

        Returns:
            True: 처리 성공 (ACK 대상)
            False: 처리 실패 (pending 유지)
        """
        message_id = message.get("id", "unknown")

        try:
            jd_input = parse_jd_preprocess_message(message)

            logger.info(
                "[TEXT_WORKER] Processing | id=%s requestId=%s brand=%s position=%s",
                message_id,
                jd_input.request_id,
                jd_input.brand_name,
                jd_input.position_name,
            )

            self.jd_worker.process(jd_input)

            logger.info(
                "[TEXT_WORKER] Success | id=%s requestId=%s",
                message_id, jd_input.request_id
            )

            return True

        except MessageParseError as e:
            logger.error(
                "[TEXT_WORKER] Message parse error | id=%s error=%s",
                message_id, str(e)
            )
            return True  # 파싱 오류는 재시도해도 실패하므로 ACK

        except Exception as e:
            logger.error(
                "[TEXT_WORKER] Processing failed | id=%s error=%s",
                message_id, str(e),
                exc_info=True
            )
            return False  # 처리 실패는 pending 유지
