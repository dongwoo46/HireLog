package com.hirelog.api.company.application

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.company.application.port.CompanyCandidateQuery
import com.hirelog.api.company.application.view.CompanyCandidateDetailView
import com.hirelog.api.company.application.view.CompanyCandidateListView
import com.hirelog.api.company.presentation.controller.dto.CompanyCandidateSearchReq
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * CompanyCandidateReadService
 *
 * 책임:
 * - CompanyCandidate 조회 유스케이스 전담
 * - 목록 / 상세 조회
 *
 * 설계 원칙:
 * - 비즈니스 규칙 ❌
 * - 상태 변경 ❌
 * - Query Port 위임만 수행
 */
@Service
@Transactional(readOnly = true)
class CompanyCandidateReadService(
    private val companyCandidateQuery: CompanyCandidateQuery
) {

    /**
     * CompanyCandidate 목록 조회
     */
    fun search(
        condition: CompanyCandidateSearchReq,
        pageable: Pageable
    ): PagedResult<CompanyCandidateListView> {

        val pageResult = companyCandidateQuery.search(condition, pageable)

        return PagedResult.of(
            items = pageResult.content,
            page = pageable.pageNumber,
            size = pageable.pageSize,
            totalElements = pageResult.totalElements
        )
    }

    /**
     * CompanyCandidate 상세 조회
     */
    fun getDetail(candidateId: Long): CompanyCandidateDetailView? {
        return companyCandidateQuery.findDetailById(candidateId)
    }
}
