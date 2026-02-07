package com.hirelog.api.relation.application.brandposition.port

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.relation.application.brandposition.view.BrandPositionListView
import com.hirelog.api.relation.presentation.controller.dto.BrandPositionSearchReq
import org.springframework.data.domain.Pageable

/**
 * BrandPosition Query Port
 *
 * 역할:
 * - BrandPosition 조회 전용 (Read Context)
 *
 * 원칙:
 * - 도메인 엔티티 반환 금지
 * - View / Boolean / Count 만 허용
 */
interface BrandPositionQuery {

    /**
     * BrandPosition 검색 (Admin)
     */
    fun search(
        condition: BrandPositionSearchReq,
        pageable: Pageable
    ): PagedResult<BrandPositionListView>

    /**
     * Brand + Position 조합 존재 여부
     * - 중복 생성 검증용
     */
    fun existsByBrandIdAndPositionId(
        brandId: Long,
        positionId: Long
    ): Boolean

    /**
     * BrandPosition 단건 존재 여부
     */
    fun existsById(id: Long): Boolean
}
