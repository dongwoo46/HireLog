package com.hirelog.api.job.application.summary

import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.job.application.summary.query.JobSummarySearchQuery
import com.hirelog.api.job.application.summary.query.JobSummarySearchResult
import com.hirelog.api.job.application.summary.view.JobSummaryDetailView
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryOpenSearchQuery
import com.hirelog.api.relation.application.memberjobsummary.port.MemberJobSummaryQuery
import org.springframework.stereotype.Service

/**
 * JobSummary Read Service
 *
 * 책임:
 * - JobSummary 조회 유스케이스 처리
 * - Controller가 infra 레이어(OpenSearch)를 직접 참조하지 않도록 격리
 *
 * 설계:
 * - search: OpenSearch 검색 후 DB two-query로 사용자 저장 상태 enrichment
 * - getDetail: JPA 단건 조회 (저장 상태 포함 이미 처리됨)
 *
 * Cross-domain 참조:
 * - MemberJobSummaryQuery(relation 도메인)를 read-only 목적으로 참조
 */
@Service
class JobSummaryReadService(
    private val jobSummaryQuery: JobSummaryQuery,
    private val openSearchQuery: JobSummaryOpenSearchQuery,
    private val memberJobSummaryQuery: MemberJobSummaryQuery
) {

    fun search(query: JobSummarySearchQuery, memberId: Long): JobSummarySearchResult {
        val result = openSearchQuery.search(query)

        if (result.items.isEmpty()) return result

        val jobSummaryIds = result.items.map { it.id }.toSet()
        val savedStates = memberJobSummaryQuery.findSavedStatesByJobSummaryIds(
            memberId = memberId,
            jobSummaryIds = jobSummaryIds
        )

        val enrichedItems = result.items.map { item ->
            val state = savedStates[item.id]
            item.copy(
                isSaved = state != null,
                memberJobSummaryId = state?.memberJobSummaryId,
                memberSaveType = state?.saveType?.name
            )
        }

        return result.copy(items = enrichedItems)
    }

    fun getDetail(jobSummaryId: Long, memberId: Long): JobSummaryDetailView? {
        return jobSummaryQuery.findDetailById(jobSummaryId, memberId)
    }
}
