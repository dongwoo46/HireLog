package com.hirelog.api.job.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Lob

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

    /**
     * 이상적인 지원자 프로필
     * 2~4문장, 기술 프로필 + 문제해결 스타일 + 협업 특성
     */
    @Lob
    @Column(name = "ideal_candidate", updatable = false)
    val idealCandidate: String? = null,

    /**
     * 필수 신호/역량
     * newline-separated, 필수 자격에서 추출한 비협상 요건
     */
    @Lob
    @Column(name = "must_have_signals", updatable = false)
    val mustHaveSignals: String? = null,

    /**
     * 준비 포인트
     * newline-separated, 이력서/면접에서 강조할 키워드
     */
    @Lob
    @Column(name = "preparation_focus", updatable = false)
    val preparationFocus: String? = null,

    /**
     * 전환 가능 강점 및 보완 계획
     * Format: "[강점]\n...\n\n[보완]\n..."
     */
    @Lob
    @Column(name = "transferable_strengths_and_gap_plan", updatable = false)
    val transferableStrengthsAndGapPlan: String? = null,

    /**
     * 증거 및 지표
     * Format: "[증거]\n...\n\n[지표]\n..."
     */
    @Lob
    @Column(name = "proof_points_and_metrics", updatable = false)
    val proofPointsAndMetrics: String? = null,

    /**
     * 스토리 앵글
     * newline-separated, 문제→접근→해결→결과 서사
     */
    @Lob
    @Column(name = "story_angles", updatable = false)
    val storyAngles: String? = null,

    /**
     * 핵심 도전과제
     * newline-separated, JD에서 추론한 핵심 미션/문제
     */
    @Lob
    @Column(name = "key_challenges", updatable = false)
    val keyChallenges: String? = null,

    /**
     * 기술적 맥락
     * 2~4문장, 기술 노력이 집중되는 영역
     */
    @Lob
    @Column(name = "technical_context", updatable = false)
    val technicalContext: String? = null,

    /**
     * 면접 질문
     * newline-separated, 중립적 면접 질문 목록
     */
    @Lob
    @Column(name = "questions_to_ask", updatable = false)
    val questionsToAsk: String? = null,

    /**
     * 고려사항
     * newline-separated, 현실적 준비 포인트
     */
    @Lob
    @Column(name = "considerations", updatable = false)
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
