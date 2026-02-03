package com.hirelog.api.company.application.port

import com.hirelog.api.company.application.view.CompanyNameView
import com.hirelog.api.company.application.view.CompanyView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * CompanyQuery
 *
 * 책임:
 * - Company 조회 전용 (View)
 */
interface CompanyQuery {

    fun findViewById(id: Long): CompanyView?

    fun findAllViews(pageable: Pageable): Page<CompanyView>

    fun findAllActiveViews(pageable: Pageable): Page<CompanyView>

    fun findAllNames(): List<CompanyNameView>
}
