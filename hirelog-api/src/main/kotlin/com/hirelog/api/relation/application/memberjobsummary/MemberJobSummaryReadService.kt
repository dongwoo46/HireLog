package com.hirelog.api.relation.application.memberjobsummary

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.relation.application.memberjobsummary.port.MemberJobSummaryQuery
import com.hirelog.api.relation.application.memberjobsummary.view.HiringStageView
import com.hirelog.api.relation.application.memberjobsummary.view.MemberJobSummaryListView
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import org.springframework.stereotype.Service

/**
 * MemberJobSummary Read Service
 *
 * 책임:
 * - 조회 유스케이스 처리
 * - Read Port 위임
 *
 * 규칙:
 * - Entity 접근 금지
 * - 트랜잭션 직접 관리하지 않음
 */
@Service
class MemberJobSummaryReadService(
    private val query: MemberJobSummaryQuery
) {

    fun getMySummaries(
        memberId: Long,
        saveType: MemberJobSummarySaveType?,
        page: Int,
        size: Int
    ): PagedResult<MemberJobSummaryListView> {
        return query.findMySummaries(
            memberId = memberId,
            saveType = saveType,
            page = page,
            size = size
        )
    }

    fun getStages(
        memberId: Long,
        jobSummaryId: Long
    ): List<HiringStageView> {
        return query.findStages(
            memberId = memberId,
            jobSummaryId = jobSummaryId
        )
    }

    fun exists(
        memberId: Long,
        jobSummaryId: Long
    ): Boolean {
        return query.exists(
            memberId = memberId,
            jobSummaryId = jobSummaryId
        )
    }
}
