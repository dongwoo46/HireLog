package com.hirelog.api.job.application.intake

import com.hirelog.api.common.infra.redis.messaging.RedisStreamPublisher
import com.hirelog.api.common.infra.redis.messaging.RedisStreamSerializer
import com.hirelog.api.common.infra.storage.FileStorageService
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.infra.redis.JdStreamKeys
import com.hirelog.api.job.domain.JobSourceType
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

/**
 * OCR 기반 JD 수집 서비스
 *
 * 책임:
 * - 업로드된 이미지 파일 저장
 * - OCR 전처리 요청 메시지 발행
 *
 * 비책임:
 * - OCR 처리 ❌ (Python에서 수행)
 * - 중복 판단 ❌ (Facade에서 수행)
 */
@Service
class OcrJdIntakeService(
    private val fileStorageService: FileStorageService,
    private val redisStreamPublisher: RedisStreamPublisher
) {

    /**
     * OCR JD 요약 요청
     *
     * @param brandName 브랜드명
     * @param positionName 포지션명
     * @param imageFiles 이미지 파일 목록
     * @return requestId
     */
    fun requestOcrSummary(
        brandName: String,
        positionName: String,
        imageFiles: List<MultipartFile>
    ): String {
        require(brandName.isNotBlank()) { "brandName must not be blank" }
        require(positionName.isNotBlank()) { "positionName must not be blank" }
        require(imageFiles.isNotEmpty()) { "imageFiles must not be empty" }

        val requestId = UUID.randomUUID().toString()

        // 이미지 파일 저장
        val savedPaths = fileStorageService.saveImages(imageFiles, "ocr")

        log.info(
            "[OCR_INTAKE] requestId={}, brandName={}, positionName={}, imageCount={}, paths={}",
            requestId, brandName, positionName, imageFiles.size, savedPaths
        )

        // Redis Stream 메시지 발행
        val message = RedisStreamSerializer.serialize(
            metadata = mapOf(
                "type" to JdMessageType.JD_PREPROCESS_REQUEST.name,
                "requestId" to requestId,
                "brandName" to brandName,
                "positionName" to positionName,
                "createdAt" to System.currentTimeMillis().toString(),
                "messageVersion" to "v1"
            ),
            payload = mapOf(
                "source" to JobSourceType.IMAGE.name,
                "images" to savedPaths.joinToString(",")
            )
        )

        redisStreamPublisher.publish(
            streamKey = JdStreamKeys.PREPROCESS_OCR_REQUEST,
            message = message
        )

        return requestId
    }
}
