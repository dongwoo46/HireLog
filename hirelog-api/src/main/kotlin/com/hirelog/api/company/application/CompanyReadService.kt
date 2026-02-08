package com.hirelog.api.company.application

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.company.application.port.CompanyQuery
import com.hirelog.api.company.application.view.CompanyDetailView
import com.hirelog.api.company.application.view.CompanyNameView
import com.hirelog.api.company.application.view.CompanyView
import com.hirelog.api.company.presentation.controller.dto.CompanySearchReq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/**
 * Company Read Service
 *
 * 책임:
 * - Company 조회 유스케이스 전담
 * - Query Port 호출
 * - 조회 결과 가공 (Page → PagedResult)
 *
 * 특징:
 * - 인터페이스 없음 (단일 유스케이스)
 * - 트랜잭션 없음 (Read-only)
 */
@Service
class CompanyReadService(
    private val companyQuery: CompanyQuery
) {

    fun findView(companyId: Long): CompanyView? =
        companyQuery.findViewById(companyId)

    fun findDetail(companyId: Long): CompanyDetailView? =
        companyQuery.findDetailById(companyId)

    fun search(
        condition: CompanySearchReq,
        pageable: Pageable
    ): PagedResult<CompanyView> {

        val page = companyQuery.search(condition, pageable)

        return PagedResult.of(
            items = page.content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements
        )
    }

    fun findAllNames(): List<CompanyNameView> =
        companyQuery.findAllNames()
}
