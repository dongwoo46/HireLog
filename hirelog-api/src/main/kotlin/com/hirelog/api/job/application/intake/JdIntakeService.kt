package com.hirelog.api.job.application.intake

import com.hirelog.api.common.infra.storage.FileStorageService
import com.hirelog.api.job.application.intake.port.JdPreprocessRequestPort
import com.hirelog.api.job.application.messaging.JdPreprocessRequestMessage
import com.hirelog.api.job.application.summary.JobSummaryRequestWriteService
import com.hirelog.api.job.domain.type.JobSourceType
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.*

/**
 * JdIntakePolicy
 *
 * 역할:
 * - JD 유입 정책 결정자
 * - 파이프라인 진입 여부 판단
 *
 * 책임:
 * - canonicalHash 생성
 * - JD 중복 판정
 * - 요약 결과 중복 판정
 *
 * 비책임:
 * - DB 저장 ❌
 * - 상태 변경 ❌
 * - 도메인 생성 ❌
 */
@Service
class JdIntakeService(
    private val fileStorageService: FileStorageService,
    private val jdPreprocessRequestPort: JdPreprocessRequestPort,
    private val jobSummaryRequestWriteService: JobSummaryRequestWriteService
) {

    /**
     * TEXT 기반 JD 전처리 요청
     */
    fun requestText(
        memberId: Long,
        brandName: String,
        brandPositionName: String,
        text: String,
    ): String {
        require(brandName.isNotBlank()) { "brandName is required" }
        require(brandPositionName.isNotBlank()) { "positionName is required" }
        require(text.isNotBlank()) { "text is required" }

        val message = JdPreprocessRequestMessage(
            eventId = UUID.randomUUID().toString(),
            requestId = UUID.randomUUID().toString(),
            occurredAt = System.currentTimeMillis(),
            version = "v1",
            brandName = brandName,
            positionName = brandPositionName,
            source = JobSourceType.TEXT,
            text = text,
        )

        jdPreprocessRequestPort.send(message)
        jobSummaryRequestWriteService.createRequest(memberId, message.requestId)
        return message.requestId
    }

    /**
     * OCR 기반 JD 전처리 요청
     */
    fun requestOcr(
        memberId: Long,
        brandName: String,
        brandPositionName: String,
        imageFiles: List<MultipartFile>,
    ): String {
        require(brandName.isNotBlank()) { "brandName is required" }
        require(brandPositionName.isNotBlank()) { "positionName is required" }
        require(imageFiles.isNotEmpty()) { "imageFiles is required" }

        val savedPaths = fileStorageService.saveImages(imageFiles, "ocr")

        val message = JdPreprocessRequestMessage(
            eventId = UUID.randomUUID().toString(),
            requestId = UUID.randomUUID().toString(),
            occurredAt = System.currentTimeMillis(),
            version = "v1",
            brandName = brandName,
            positionName = brandPositionName,
            source = JobSourceType.IMAGE,
            images = savedPaths,
        )

        jdPreprocessRequestPort.send(message)
        jobSummaryRequestWriteService.createRequest(memberId, message.requestId)
        return message.requestId
    }

    /**
     * URL 기반 JD 전처리 요청
     */
    fun requestUrl(
        memberId: Long,
        brandName: String,
        brandPositionName: String,
        url: String,
    ): String {
        require(brandName.isNotBlank()) { "brandName is required" }
        require(brandPositionName.isNotBlank()) { "positionName is required" }
        require(isValidUrl(url)) { "Invalid URL format: $url" }

        val message = JdPreprocessRequestMessage(
            eventId = UUID.randomUUID().toString(),
            requestId = UUID.randomUUID().toString(),
            occurredAt = System.currentTimeMillis(),
            version = "v1",
            brandName = brandName,
            positionName = brandPositionName,
            source = JobSourceType.URL,
            url = url,
        )

        jdPreprocessRequestPort.send(message)
        jobSummaryRequestWriteService.createRequest(memberId, message.requestId)
        return message.requestId
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = java.net.URI(url)
            uri.scheme in listOf("http", "https") && uri.host != null
        } catch (e: Exception) {
            false
        }
    }

}
