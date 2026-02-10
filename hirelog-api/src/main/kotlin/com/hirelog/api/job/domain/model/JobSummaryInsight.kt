package com.hirelog.api.job.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

/**
 * JobSummary Insight VO
 *
 * 역할:
 * - JD 분석 기반 지원자 준비 인사이트
 * - 검색/분석에 활용되는 메타데이터
 *
 * 설계:
 * - @Embeddable로 JobSummary에 포함
 * - 모든 필드 nullable (LLM 추출 실패 가능)
 * - 불변 객체
 */
@Embeddable
class JobSummaryInsight protected constructor(

    @Column(name = "ideal_candidate", updatable = false, columnDefinition = "TEXT")
    val idealCandidate: String? = null,

    @Column(name = "must_have_signals", updatable = false, columnDefinition = "TEXT")
    val mustHaveSignals: String? = null,

    @Column(name = "preparation_focus", updatable = false, columnDefinition = "TEXT")
    val preparationFocus: String? = null,

    @Column(name = "transferable_strengths_and_gap_plan", updatable = false, columnDefinition = "TEXT")
    val transferableStrengthsAndGapPlan: String? = null,

    @Column(name = "proof_points_and_metrics", updatable = false, columnDefinition = "TEXT")
    val proofPointsAndMetrics: String? = null,

    @Column(name = "story_angles", updatable = false, columnDefinition = "TEXT")
    val storyAngles: String? = null,

    @Column(name = "key_challenges", updatable = false, columnDefinition = "TEXT")
    val keyChallenges: String? = null,

    @Column(name = "technical_context", updatable = false, columnDefinition = "TEXT")
    val technicalContext: String? = null,

    @Column(name = "questions_to_ask", updatable = false, columnDefinition = "TEXT")
    val questionsToAsk: String? = null,

    @Column(name = "considerations", updatable = false, columnDefinition = "TEXT")
    val considerations: String? = null

) {
    companion object {

        fun create(
            idealCandidate: String?,
            mustHaveSignals: String?,
            preparationFocus: String?,
            transferableStrengthsAndGapPlan: String?,
            proofPointsAndMetrics: String?,
            storyAngles: String?,
            keyChallenges: String?,
            technicalContext: String?,
            questionsToAsk: String?,
            considerations: String?
        ): JobSummaryInsight {
            return JobSummaryInsight(
                idealCandidate = idealCandidate,
                mustHaveSignals = mustHaveSignals,
                preparationFocus = preparationFocus,
                transferableStrengthsAndGapPlan = transferableStrengthsAndGapPlan,
                proofPointsAndMetrics = proofPointsAndMetrics,
                storyAngles = storyAngles,
                keyChallenges = keyChallenges,
                technicalContext = technicalContext,
                questionsToAsk = questionsToAsk,
                considerations = considerations
            )
        }

        /**
         * 빈 Insight 생성 (LLM 추출 실패 시)
         */
        fun empty(): JobSummaryInsight = JobSummaryInsight()
    }
}
