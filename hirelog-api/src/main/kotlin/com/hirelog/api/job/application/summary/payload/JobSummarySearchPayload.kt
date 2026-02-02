package com.hirelog.api.job.application.summary.payload

import com.hirelog.api.job.domain.JobSummary
import java.time.LocalDateTime

/**
 * JobSummary OpenSearch 인덱싱용 Payload
 *
 * 용도:
 * - OutboxEvent.payload에 JSON 직렬화되어 저장
 * - Debezium CDC → Kafka → OpenSearch Consumer 흐름에서 사용
 *
 * 설계 원칙:
 * - 검색에 필요한 필드만 포함 (llmProvider, llmModel 제외)
 * - Enum은 String으로 직렬화하여 호환성 확보
 * - Insight 필드는 flat하게 저장 (검색 효율성)
 */
data class JobSummarySearchPayload(
    val id: Long,
    val jobSnapshotId: Long,
    val brandId: Long,
    val brandName: String,
    val companyId: Long?,
    val companyName: String?,
    val positionId: Long,
    val positionName: String,
    val brandPositionName: String?,
    val careerType: String,
    val careerYears: String?,
    val summaryText: String,
    val responsibilities: String,
    val requiredQualifications: String,
    val preferredQualifications: String?,
    val techStack: String?,
    val techStackParsed: List<String>?,
    val recruitmentProcess: String?,

    // === Insight 필드 (flat) ===
    val idealCandidate: String?,
    val mustHaveSignals: String?,
    val preparationFocus: String?,
    val transferableStrengthsAndGapPlan: String?,
    val proofPointsAndMetrics: String?,
    val storyAngles: String?,
    val keyChallenges: String?,
    val technicalContext: String?,
    val questionsToAsk: String?,
    val considerations: String?,

    val createdAt: LocalDateTime
) {
    companion object {

        /**
         * JobSummary → Payload 변환
         */
        fun from(entity: JobSummary): JobSummarySearchPayload {
            return JobSummarySearchPayload(
                id = entity.id,
                jobSnapshotId = entity.jobSnapshotId,
                brandId = entity.brandId,
                brandName = entity.brandName,
                companyId = entity.companyId,
                companyName = entity.companyName,
                positionId = entity.positionId,
                positionName = entity.positionName,
                brandPositionName = entity.brandPositionName,
                careerType = entity.careerType.name,
                careerYears = entity.careerYears,
                summaryText = entity.summaryText,
                responsibilities = entity.responsibilities,
                requiredQualifications = entity.requiredQualifications,
                preferredQualifications = entity.preferredQualifications,
                techStack = entity.techStack,
                techStackParsed = parseTechStack(entity.techStack),
                recruitmentProcess = entity.recruitmentProcess,

                // Insight
                idealCandidate = entity.insight.idealCandidate,
                mustHaveSignals = entity.insight.mustHaveSignals,
                preparationFocus = entity.insight.preparationFocus,
                transferableStrengthsAndGapPlan = entity.insight.transferableStrengthsAndGapPlan,
                proofPointsAndMetrics = entity.insight.proofPointsAndMetrics,
                storyAngles = entity.insight.storyAngles,
                keyChallenges = entity.insight.keyChallenges,
                technicalContext = entity.insight.technicalContext,
                questionsToAsk = entity.insight.questionsToAsk,
                considerations = entity.insight.considerations,

                createdAt = entity.createdAt
            )
        }

        /**
         * techStack CSV 파싱
         *
         * 규칙:
         * - 콤마(,) 구분자로 split
         * - 각 항목 trim
         * - 빈 문자열 제거
         * - 파싱 불가 시 null 반환
         */
        private fun parseTechStack(techStack: String?): List<String>? {
            if (techStack.isNullOrBlank()) return null

            val parsed = techStack
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            // 파싱 결과가 1개이고 원본과 동일하면 자연어로 판단
            if (parsed.size == 1 && parsed[0] == techStack.trim()) {
                return null
            }

            return parsed.ifEmpty { null }
        }
    }
}
