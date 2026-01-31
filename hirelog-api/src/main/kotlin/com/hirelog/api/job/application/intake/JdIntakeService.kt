package com.hirelog.api.job.application.intake

import com.hirelog.api.common.infra.storage.FileStorageService
import com.hirelog.api.job.application.intake.port.JdPreprocessRequestPort
import com.hirelog.api.job.application.messaging.JdPreprocessRequestMessage
import com.hirelog.api.job.domain.JobSourceType
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
) {

    /**
     * TEXT 기반 JD 전처리 요청
     */
    fun requestText(
        brandName: String,
        positionName: String,
        text: String,
    ): String {
        return send(
            brandName = brandName,
            positionName = positionName,
            source = JobSourceType.TEXT,
            payload = text,
        )
    }

    /**
     * OCR 기반 JD 전처리 요청
     */
    fun requestOcr(
        brandName: String,
        positionName: String,
        imageFiles: List<MultipartFile>,
    ): String {
        val savedPaths = fileStorageService.saveImages(imageFiles, "ocr")

        return send(
            brandName = brandName,
            positionName = positionName,
            source = JobSourceType.IMAGE,
            payload = savedPaths.joinToString(","),
        )
    }

    /**
     * URL 기반 JD 전처리 요청
     */
    fun requestUrl(
        brandName: String,
        positionName: String,
        url: String,
    ): String {
        require(isValidUrl(url)) { "Invalid URL format: $url" }

        return send(
            brandName = brandName,
            positionName = positionName,
            source = JobSourceType.URL,
            payload = url,
        )
    }

    /**
     * 공통 전처리 요청 로직
     */
    private fun send(
        brandName: String,
        positionName: String,
        source: JobSourceType,
        payload: String,
    ): String {
        require(brandName.isNotBlank())
        require(positionName.isNotBlank())
        require(payload.isNotBlank())

        val message = JdPreprocessRequestMessage(
            eventId = UUID.randomUUID().toString(),
            requestId = UUID.randomUUID().toString(),
            occurredAt = System.currentTimeMillis(),
            version = "v1",
            brandName = brandName,
            positionName = positionName,
            source = source,
            text = payload,
        )

        jdPreprocessRequestPort.send(message)

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
