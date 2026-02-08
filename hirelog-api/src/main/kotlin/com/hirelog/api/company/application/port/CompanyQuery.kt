package com.hirelog.api.company.application.port

import com.hirelog.api.company.application.view.CompanyDetailView
import com.hirelog.api.company.application.view.CompanyNameView
import com.hirelog.api.company.application.view.CompanyView
import com.hirelog.api.company.presentation.controller.dto.CompanySearchReq
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * Company Query Port
 *
 * 책임:
 * - Company 조회 전담
 */
interface CompanyQuery {

    fun existsByNormalizedName(normalizedName: String): Boolean

    fun findViewById(companyId: Long): CompanyView?

    fun findDetailById(companyId: Long): CompanyDetailView?

    fun search(
        condition: CompanySearchReq,
        pageable: Pageable
    ): Page<CompanyView>

    /**
     * Company 전체 이름 목록 조회
     *
     * 용도:
     * - 중복 검사
     * - 자동 완성
     * - 사전 로딩
     */
    fun findAllNames(): List<CompanyNameView>
}
