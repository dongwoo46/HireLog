package com.hirelog.api.job.application.summary.payload

import com.hirelog.api.job.domain.JobSummary
import java.time.format.DateTimeFormatter

/**
 * JobSummary Outbox 메시지 계약 DTO
 *
 * 용도:
 * - OutboxEvent.payload에 JSON 직렬화되어 저장
 * - Debezium CDC → Kafka 흐름에서 사용
 *
 * 설계 원칙:
 * - 모든 필드는 직렬화 친화적 타입 (String, Long, List<String>)
 * - LocalDateTime → ISO-8601 String 변환
 * - Domain 타입 직접 노출 금지
 */
data class JobSummaryOutboxPayload(
    val id: Long,
    val jobSnapshotId: Long,
    val brandId: Long,
    val brandName: String,
    val companyId: Long?,
    val companyName: String?,
    val positionId: Long,
    val positionName: String,
    val brandPositionId: Long?,
    val brandPositionName: String?,
    val positionCategoryId: Long,
    val positionCategoryName: String,
    val careerType: String,
    val careerYears: String?,
    val summaryText: String,
    val responsibilities: String,
    val requiredQualifications: String,
    val preferredQualifications: String?,
    val techStack: String?,
    val techStackParsed: List<String>?,
    val recruitmentProcess: String?,

    // === Insight 필드 ===
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

    val createdAt: String  // ISO-8601 형식
) {
    companion object {

        private val ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        /**
         * JobSummary → OutboxPayload 변환
         *
         * 변환 규칙:
         * - Enum → name (String)
         * - LocalDateTime → ISO-8601 String
         */
        fun from(entity: JobSummary): JobSummaryOutboxPayload {
            return JobSummaryOutboxPayload(
                id = entity.id,
                jobSnapshotId = entity.jobSnapshotId,
                brandId = entity.brandId,
                brandName = entity.brandName,
                companyId = entity.companyId,
                companyName = entity.companyName,
                positionId = entity.positionId,
                positionName = entity.positionName,
                brandPositionId = entity.brandPositionId,
                brandPositionName = entity.brandPositionName,
                positionCategoryId = entity.positionCategoryId,
                positionCategoryName = entity.positionCategoryName,
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

                createdAt = entity.createdAt.format(ISO_FORMATTER)
            )
        }

        private fun parseTechStack(techStack: String?): List<String>? {
            if (techStack.isNullOrBlank()) return null

            val parsed = techStack
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            if (parsed.size == 1 && parsed[0] == techStack.trim()) {
                return null
            }

            return parsed.ifEmpty { null }
        }
    }
}
