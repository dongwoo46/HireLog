package com.hirelog.api.brand.application.port

import com.hirelog.api.brand.application.view.BrandDetailView
import com.hirelog.api.brand.application.view.BrandListView
import com.hirelog.api.brand.presentation.controller.dto.BrandSearchReq
import com.hirelog.api.common.application.port.PagedResult
import org.springframework.data.domain.Pageable

interface BrandQuery {

    /**
     * 브랜드 목록 조회 (검색 + 페이징)
     */
    fun search(
        condition: BrandSearchReq,
        pageable: Pageable
    ): PagedResult<BrandListView>

    /**
     * 브랜드 상세 조회
     *
     * - Company + BrandPosition 포함
     */
    fun findDetailById(brandId: Long): BrandDetailView?

    /**
     * Write 선행 검증
     */
    fun existsById(brandId: Long): Boolean

    fun existsByNormalizedName(normalizedName: String): Boolean

}
