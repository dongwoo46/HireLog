// application/messaging/mapper/JobSummaryPreprocessResponseMessageMapper.kt
package com.hirelog.api.job.application.messaging.mapper

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hirelog.api.job.application.messaging.JdPreprocessResponseMessage
import com.hirelog.api.job.domain.JobSourceType
import com.hirelog.api.job.domain.RecruitmentPeriodType
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Redis Stream Message → Contract DTO Mapper
 *
 * 책임:
 * - Map<String, String> → Typed Message
 *
 * 중요:
 * - ObjectMapper 의존성은 여기서만 존재
 * - 실패 시 즉시 예외 (fail-fast)
 */
@Component
class JdPreprocessResponseMessageMapper(
    private val objectMapper: ObjectMapper
) {

    fun from(message: Map<String, String>): JdPreprocessResponseMessage {

        val canonicalMap =
            message["payload.canonicalMap"]
                ?.let {
                    objectMapper.readValue(
                        it,
                        object : TypeReference<Map<String, List<String>>>() {}
                    )
                }
                ?: error("payload.canonicalMap is missing")

        val skills =
            message["payload.skills"]
                ?.let {
                    objectMapper.readValue(
                        it,
                        object : TypeReference<List<String>>() {}
                    )
                }
                ?: emptyList()

        return JdPreprocessResponseMessage(
            type = message["meta.type"] ?: error("meta.type missing"),
            messageVersion = message["meta.messageVersion"] ?: error("meta.messageVersion missing"),
            createdAt = message["meta.createdAt"]!!.toLong(),

            requestId = message["meta.requestId"]!!,

            brandName = message["meta.brandName"]!!,
            positionName = message["meta.positionName"]!!,

            source = JobSourceType.valueOf(message["payload.source"]!!),
            sourceUrl = message["payload.sourceUrl"],

            canonicalMap = canonicalMap,

            recruitmentPeriodType =
            RecruitmentPeriodType.valueOf(message["payload.recruitmentPeriodType"]!!),

            openedDate = message["payload.recruitmentOpenDate"]?.let(LocalDate::parse),
            closedDate = message["payload.recruitmentCloseDate"]?.let(LocalDate::parse),

            skills = skills
        )
    }
}
