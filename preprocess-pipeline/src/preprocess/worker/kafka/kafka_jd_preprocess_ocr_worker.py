# src/preprocess/worker/kafka/kafka_jd_preprocess_ocr_worker.py

"""
OCR JD 전처리 Worker

책임:
- OCR 수행 + 전처리 파이프라인 실행
- 전처리 결과를 Kafka Event DTO로 변환

비책임:
- Kafka 메시지 소비
- Kafka 결과 발행
- commit / retry 판단
"""

import logging

from inputs.kafka_jd_preprocess_input import KafkaJdPreprocessInput
from outputs.kafka_jd_preprocess_output import KafkaJdPreprocessOutput
from preprocess.worker.kafka.kafka_base_jd_preprocess_worker import KafkaBaseJdPreprocessWorker
from preprocess.worker.pipeline.ocr_pipeline import OcrPipeline
from common.exceptions import ProcessingError, ErrorCode

logger = logging.getLogger(__name__)


class KafkaJdPreprocessOcrWorker(KafkaBaseJdPreprocessWorker):
    """
    Kafka 기반 OCR JD 전처리 Worker

    execute()에서 결과 DTO만 반환한다.
    Kafka 발행은 상위 BaseKafkaWorker에서 처리한다.
    """

    def __init__(self):
        super().__init__()
        self.pipeline = OcrPipeline()
        logger.info("[KAFKA_OCR_WORKER_INIT] OCR pipeline initialized")

    def execute(self, input: KafkaJdPreprocessInput) -> KafkaJdPreprocessOutput:
        """
        OCR JD 전처리 실행

        Returns:
            KafkaJdPreprocessOutput: 전처리 결과 DTO

        Raises:
            ProcessingError: 전처리 실패 시
        """
        try:
            # ==================================================
            # 1. OCR + Preprocess Pipeline 실행
            # ==================================================
            result = self.pipeline.process(input)

            canonical_map = result["canonical_map"]
            document_meta = result.get("document_meta")

            # ==================================================
            # 2. Recruitment Period 추출 (읽기 전용)
            # ==================================================
            period = (
                document_meta.recruitment_period
                if document_meta and document_meta.recruitment_period
                else None
            )

            # ==================================================
            # 3. Skills 추출 (읽기 전용)
            # ==================================================
            skill_set = (
                document_meta.skill_set
                if document_meta and document_meta.skill_set
                else None
            )

            # ==================================================
            # 4. Kafka Event DTO 생성
            # ==================================================
            output = KafkaJdPreprocessOutput.from_domain(
                request_id=input.request_id,
                brand_name=input.brand_name,
                position_name=input.position_name,
                source=input.source,
                canonical_map=canonical_map,
                recruitment_period_type=period.period_type if period else None,
                opened_date=self._normalize_date(period.open_date) if period else None,
                closed_date=self._normalize_date(period.close_date) if period else None,
                skills=skill_set.skills if skill_set else None,
                source_url=None,  # OCR은 source_url 없음
            )

            logger.info(
                "[KAFKA_JD_OCR_PREPROCESS_SUCCESS] requestId=%s brand=%s position=%s",
                input.request_id,
                input.brand_name,
                input.position_name,
            )

            return output

        except ProcessingError:
            raise
        except Exception as e:
            raise ProcessingError(
                error_code=ErrorCode.PIPELINE_OCR_001,
                message=f"OCR 전처리 실패: {str(e)}",
                cause=e,
            )
