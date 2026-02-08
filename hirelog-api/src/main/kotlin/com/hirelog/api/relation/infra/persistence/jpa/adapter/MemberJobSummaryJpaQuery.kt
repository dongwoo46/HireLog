package com.hirelog.api.relation.infra.persistence.adapter

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.relation.application.memberjobsummary.port.MemberJobSummaryQuery
import com.hirelog.api.relation.application.memberjobsummary.view.MemberJobSummaryDetailView
import com.hirelog.api.relation.application.memberjobsummary.view.MemberJobSummaryListView
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import com.hirelog.api.relation.infra.persistence.jpa.repository.MemberJobSummaryJpaQueryDsl
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * MemberJobSummary Query Adapter
 *
 * 책임:
 * - Read Port 구현
 * - Querydsl Repository 위임
 */
@Component
@Transactional(readOnly = true)
class MemberJobSummaryJpaQuery(
    private val querydslRepository: MemberJobSummaryJpaQueryDsl
) : MemberJobSummaryQuery {

    override fun findMySummaries(
        memberId: Long,
        saveType: MemberJobSummarySaveType?,
        page: Int,
        size: Int
    ): PagedResult<MemberJobSummaryListView> {
        return querydslRepository.findMySummaries(
            memberId = memberId,
            saveType = saveType,
            page = page,
            size = size
        )
    }

    override fun findDetail(
        memberId: Long,
        jobSummaryId: Long
    ): MemberJobSummaryDetailView {
        return querydslRepository.findDetail(
            memberId = memberId,
            jobSummaryId = jobSummaryId
        ) ?: throw NoSuchElementException(
            "MemberJobSummary not found (memberId=$memberId, jobSummaryId=$jobSummaryId)"
        )
    }

    override fun exists(
        memberId: Long,
        jobSummaryId: Long
    ): Boolean {
        return querydslRepository.exists(
            memberId = memberId,
            jobSummaryId = jobSummaryId
        )
    }
}
