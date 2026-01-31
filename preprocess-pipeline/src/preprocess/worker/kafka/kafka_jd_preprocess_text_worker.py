# src/preprocess/worker/kafka/kafka_jd_preprocess_text_worker.py

import logging

from inputs.kafka_jd_preprocess_input import KafkaJdPreprocessInput
from outputs.kafka_jd_preprocess_output import KafkaJdPreprocessOutput
from preprocess.worker.kafka.kafka_base_jd_preprocess_worker import KafkaBaseJdPreprocessWorker
from preprocess.worker.pipeline.text_preprocess_pipeline import TextPreprocessPipeline
from infra.kafka.kafka_producer import KafkaStreamProducer

logger = logging.getLogger(__name__)


class KafkaJdPreprocessTextWorker(KafkaBaseJdPreprocessWorker):
    """
    Kafka 기반 TEXT JD 전처리 Worker

    책임:
    - TEXT JD 전처리 파이프라인 실행
    - 전처리 결과를 Kafka Event DTO로 변환
    - Kafka로 결과 발행

    비책임:
    - Kafka 메시지 소비 ❌
    - 메시지 파싱 ❌
    - commit / retry 판단 ❌
    """

    def __init__(self, kafka_producer: KafkaStreamProducer, result_topic: str):
        super().__init__(kafka_producer, result_topic)
        self.pipeline = TextPreprocessPipeline()

    def process(self, input: KafkaJdPreprocessInput) -> KafkaJdPreprocessOutput:
        """
        TEXT JD 전처리 실행

        규칙:
        - Kafka publish 실패 시 반드시 예외 발생
        - 예외는 상위 Consumer가 재처리 판단
        """

        # ==================================================
        # 1️⃣ Pipeline 실행
        # ==================================================
        result = self.pipeline.process(input)

        canonical_map = result["canonical_map"]
        document_meta = result.get("document_meta")

        # ==================================================
        # 2️⃣ Recruitment Period (읽기 전용)
        # ==================================================
        period = (
            document_meta.recruitment_period
            if document_meta and document_meta.recruitment_period
            else None
        )

        # ==================================================
        # 3️⃣ Skills (읽기 전용)
        # ==================================================
        skill_set = (
            document_meta.skill_set
            if document_meta and document_meta.skill_set
            else None
        )

        # ==================================================
        # 4️⃣ Kafka Event DTO 생성
        # ==================================================
        output = KafkaJdPreprocessOutput.from_domain(
            request_id=input.request_id,
            brand_name=input.brand_name,
            position_name=input.position_name,
            source=input.source,
            canonical_map=canonical_map,

            # Recruitment Info
            recruitment_period_type=period.period_type if period else None,
            opened_date=period.open_date if period else None,
            closed_date=period.close_date if period else None,

            # Skills
            skills=skill_set.skills if skill_set else None,

            # TEXT는 source_url 없음
            source_url=None,
        )

        # ==================================================
        # 5️⃣ Kafka로 결과 발행
        # - 실패 시 예외 발생 (필수)
        # ==================================================
        success = self._publish_result(output)
        if not success:
            raise Exception("Kafka publish failed")

        logger.info(
            "[KAFKA_JD_TEXT_PREPROCESS_SUCCESS] requestId=%s brand=%s position=%s",
            input.request_id,
            input.brand_name,
            input.position_name,
        )

        return output
