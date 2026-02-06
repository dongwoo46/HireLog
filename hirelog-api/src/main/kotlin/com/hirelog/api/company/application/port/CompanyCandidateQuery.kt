package com.hirelog.api.company.application.port

import com.hirelog.api.company.application.view.CompanyCandidateDetailView
import com.hirelog.api.company.application.view.CompanyCandidateListView
import com.hirelog.api.company.presentation.controller.dto.CompanyCandidateSearchReq
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * CompanyCandidateQuery
 *
 * 책임:
 * - CompanyCandidate 조회 전담
 * - 목록 / 상세 조회
 */
interface CompanyCandidateQuery {

    fun search(
        condition: CompanyCandidateSearchReq,
        pageable: Pageable
    ): Page<CompanyCandidateListView>

    fun findDetailById(candidateId: Long): CompanyCandidateDetailView?

    /**
     * Brand + normalizedName 기준 중복 여부 확인
     *
     * 용도:
     * - 후보 생성 전 사전 검증
     */
    fun existsByBrandIdAndNormalizedName(
        brandId: Long,
        normalizedName: String
    ): Boolean
}
