package com.hirelog.api.relation.application.memberjobsummary.port

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.relation.application.memberjobsummary.view.CoverLetterView
import com.hirelog.api.relation.application.memberjobsummary.view.HiringStageView
import com.hirelog.api.relation.application.memberjobsummary.view.JobSummarySavedStateView
import com.hirelog.api.relation.application.memberjobsummary.view.MemberJobSummaryListView
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType

/**
 * MemberJobSummary Port (Read)
 *
 * 책임:
 * - MemberJobSummary 조회 전용
 * - View 기반 Projection 반환
 *
 * 규칙:
 * - Entity 반환 금지
 * - 필요한 컬럼만 조회
 */
interface MemberJobSummaryQuery {

    /**
     * 내가 저장한 JD 목록 조회
     */
    fun findMySummaries(
        memberId: Long,
        saveType: MemberJobSummarySaveType?,
        page: Int,
        size: Int
    ): PagedResult<MemberJobSummaryListView>

    /**
     * 회원이 등록한 JD(멤버-공고 관계)가 1건 이상인지 확인
     */
    fun existsAnyByMemberId(memberId: Long): Boolean

    /**
     * 채용 단계 목록 조회
     */
    fun findStages(
        memberId: Long,
        jobSummaryId: Long
    ): List<HiringStageView>

    /**
     * 특정 JD 저장 여부 확인
     */
    fun exists(
        memberId: Long,
        jobSummaryId: Long
    ): Boolean

    /**
     * 자기소개서 목록 조회
     */
    fun findCoverLetters(
        memberId: Long,
        jobSummaryId: Long
    ): List<CoverLetterView>

    /**
     * JobSummary ID 목록 기준 저장 상태 일괄 조회
     *
     * 용도:
     * - JobSummary 검색 결과 목록에 사용자 저장 상태 enrichment
     * - jobSummaryId → JobSummarySavedStateView 매핑 반환
     */
    fun findSavedStatesByJobSummaryIds(
        memberId: Long,
        jobSummaryIds: Set<Long>
    ): Map<Long, JobSummarySavedStateView>
}
