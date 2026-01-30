package com.hirelog.api.job.application.intake

import com.hirelog.api.common.infra.storage.FileStorageService
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.messaging.JdPreprocessRequestMessage
import com.hirelog.api.job.domain.JobSourceType
import com.hirelog.api.job.infra.kafka.publisher.JdPreprocessOcrRequestPublisher
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

/**
 * OCR 기반 JD 수집 서비스
 *
 * 책임:
 * - 업로드된 이미지 파일 저장
 * - OCR 전처리 요청 Kafka 이벤트 발행
 *
 * 비책임:
 * - OCR 처리 ❌ (Python Worker)
 * - 중복 판단 ❌
 * - 요약 처리 ❌
 */
@Service
class OcrJdIntakeService(
    private val fileStorageService: FileStorageService,
    private val jdPreprocessOcrRequestPublisher: JdPreprocessOcrRequestPublisher
) {

    /**
     * OCR JD 요약 요청
     *
     * @param brandName 브랜드명
     * @param positionName 포지션명
     * @param imageFiles 업로드 이미지 파일 목록
     * @return requestId (Correlation ID)
     */
    fun requestOcrSummary(
        brandName: String,
        positionName: String,
        imageFiles: List<MultipartFile>
    ): String {

        // === 입력 검증 ===
        require(brandName.isNotBlank()) { "brandName must not be blank" }
        require(positionName.isNotBlank()) { "positionName must not be blank" }
        require(imageFiles.isNotEmpty()) { "imageFiles must not be empty" }

        // === 식별자 생성 ===
        val eventId = UUID.randomUUID().toString()
        val requestId = UUID.randomUUID().toString()

        // === 이미지 파일 저장 ===
        val savedPaths = fileStorageService.saveImages(imageFiles, "ocr")

        // === Kafka Event 생성 ===
        val message = JdPreprocessRequestMessage(
            eventId = eventId,
            requestId = requestId,
            occurredAt = System.currentTimeMillis(),
            version = "v1",
            brandName = brandName,
            positionName = positionName,
            source = JobSourceType.IMAGE,
            text = savedPaths.joinToString(",") // Python Worker가 split 처리
        )

        // === Kafka 발행 (OCR Request Topic) ===
        jdPreprocessOcrRequestPublisher.publish(message)

        return requestId
    }
}
