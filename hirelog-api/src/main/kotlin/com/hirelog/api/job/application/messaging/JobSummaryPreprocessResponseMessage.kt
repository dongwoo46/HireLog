package com.hirelog.api.job.application.messaging

import com.hirelog.api.job.domain.JobSourceType
import java.time.LocalDate


/**
 * JD 전처리 결과 메시지 DTO
 *
 * Python → Spring Redis Stream 계약 객체
 *
 * 책임:
 * - Python 전처리 워커가 발행한 메시지를
 *   Spring 내부 로직으로 안전하게 전달
 *
 * 주의:
 * - 도메인 객체 아님
 * - JPA Entity ❌
 * - 외부 메시지 계약용 DTO
 */
data class JobSummaryPreprocessResponseMessage(

    // Metadata
    val type: String,                 // e.g. JD_PREPROCESS_RESULT
    val messageVersion: String,       // e.g. v1
    val createdAt: Long,              // epoch millis

    // Business Context
    val requestId: String,
    val brandName: String,
    val positionName: String,
    val source: JobSourceType,               // TEXT | OCR | URL
    val canonicalText: String,

    val sourceUrl: String? = null,
    val openedDate: LocalDate? = null,
    val closedDate: LocalDate? = null
)

fun Map<String, String>.toJobSummaryPreprocessResponseMessage(): JobSummaryPreprocessResponseMessage =
    JobSummaryPreprocessResponseMessage(
        type = this["type"]
            ?: error("type is missing"),

        messageVersion = this["messageVersion"]
            ?: error("messageVersion is missing"),

        createdAt = this["createdAt"]
            ?.toLong()
            ?: error("createdAt is missing or invalid"),

        requestId = this["requestId"]
            ?: error("requestId is missing"),

        brandName = this["brandName"]
            ?: error("brandName is missing"),

        positionName = this["positionName"]
            ?: error("positionName is missing"),

        source = this["source"]
            ?.let { JobSourceType.valueOf(it) }
            ?: error("source is missing or invalid"),

        canonicalText = this["canonicalText"]
            ?: error("canonicalText is missing"),

        sourceUrl = this["sourceUrl"]
            ?: error("sourceUrl is missing")
    )
