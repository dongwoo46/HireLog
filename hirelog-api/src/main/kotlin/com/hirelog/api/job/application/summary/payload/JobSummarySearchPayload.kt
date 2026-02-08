package com.hirelog.api.job.application.summary.payload

/**
 * JobSummary OpenSearch 인덱싱용 Payload
 *
 * 용도:
 * - Kafka Consumer → OpenSearch 인덱싱 흐름에서 사용
 *
 * 설계 원칙:
 * - 검색에 필요한 필드만 포함 (llmProvider, llmModel 제외)
 * - 모든 필드는 직렬화 친화적 타입 (String, Long, List<String>)
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

    val createdAt: String  // ISO-8601 형식
) {
    companion object {

        /**
         * OutboxPayload → SearchPayload 변환
         *
         * Kafka Consumer에서 사용
         */
        fun from(outbox: JobSummaryOutboxPayload): JobSummarySearchPayload {
            return JobSummarySearchPayload(
                id = outbox.id,
                jobSnapshotId = outbox.jobSnapshotId,
                brandId = outbox.brandId,
                brandName = outbox.brandName,
                companyId = outbox.companyId,
                companyName = outbox.companyName,
                positionId = outbox.positionId,
                positionName = outbox.positionName,
                brandPositionId = outbox.brandPositionId,
                brandPositionName = outbox.brandPositionName,
                positionCategoryId = outbox.positionCategoryId,
                positionCategoryName = outbox.positionCategoryName,
                careerType = outbox.careerType,
                careerYears = outbox.careerYears,
                summaryText = outbox.summaryText,
                responsibilities = outbox.responsibilities,
                requiredQualifications = outbox.requiredQualifications,
                preferredQualifications = outbox.preferredQualifications,
                techStack = outbox.techStack,
                techStackParsed = outbox.techStackParsed,
                recruitmentProcess = outbox.recruitmentProcess,

                // Insight
                idealCandidate = outbox.idealCandidate,
                mustHaveSignals = outbox.mustHaveSignals,
                preparationFocus = outbox.preparationFocus,
                transferableStrengthsAndGapPlan = outbox.transferableStrengthsAndGapPlan,
                proofPointsAndMetrics = outbox.proofPointsAndMetrics,
                storyAngles = outbox.storyAngles,
                keyChallenges = outbox.keyChallenges,
                technicalContext = outbox.technicalContext,
                questionsToAsk = outbox.questionsToAsk,
                considerations = outbox.considerations,

                createdAt = outbox.createdAt
            )
        }
    }
}
