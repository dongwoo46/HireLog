package com.hirelog.api.job.application.rag.port

import com.hirelog.api.job.application.rag.port.RagReviewRecord
import com.hirelog.api.job.application.rag.port.RagStageRecord
import com.hirelog.api.job.domain.type.HiringStage
import com.hirelog.api.relation.domain.type.HiringStageResult
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType

/**
 * RAG cohort 데이터 조회 포트
 *
 * 책임:
 * - STATISTICS cohort 조건 기반 jobSummaryId 목록 조회
 * - EXPERIENCE_ANALYSIS stage records 조회
 */
interface RagCohortQuery {

    /**
     * cohort 조건에 해당하는 jobSummaryId 목록 조회
     *
     * 사용: STATISTICS (cohort 필터 있을 때) → OpenSearch ids filter 용
     */
    fun findJobSummaryIdsByCohort(
        memberId: Long,
        saveType: MemberJobSummarySaveType?,
        stage: HiringStage?,
        stageResult: HiringStageResult?
    ): List<Long>

    /**
     * 사용자 채용 전형 기록 조회
     *
     * 사용: EXPERIENCE_ANALYSIS → Composer 컨텍스트 구성
     */
    fun findStageRecordsForRag(
        memberId: Long,
        stage: HiringStage?,
        stageResult: HiringStageResult?
    ): List<RagStageRecord>

    /**
     * 사용자가 작성한 공고 리뷰 조회
     *
     * 사용: EXPERIENCE_ANALYSIS → Composer 컨텍스트 구성
     */
    fun findReviewsByMemberId(memberId: Long): List<RagReviewRecord>
}