package com.hirelog.api.brand.application.query

import com.hirelog.api.brand.application.view.BrandDetailView
import com.hirelog.api.brand.application.view.BrandSummaryView
import com.hirelog.api.userrequest.application.port.PagedResult

interface BrandQuery {

    /**
     * 브랜드 목록 조회 (페이지네이션)
     * 리스트용 최소 정보
     */
    fun findAllPaged(
        page: Int,
        size: Int
    ): PagedResult<BrandSummaryView>

    /**
     * 브랜드 상세 조회
     */
    fun findDetailById(brandId: Long): BrandDetailView?

    /**
     * Write 선행 검증
     */
    fun existsById(brandId: Long): Boolean

    fun existsByNormalizedName(normalizedName: String): Boolean

}
