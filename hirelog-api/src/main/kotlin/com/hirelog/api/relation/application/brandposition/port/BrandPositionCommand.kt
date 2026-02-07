package com.hirelog.api.relation.application.brandposition.port

import com.hirelog.api.relation.domain.model.BrandPosition

/**
 * BrandPosition Command Port
 *
 * 역할:
 * - BrandPosition 도메인 엔티티 영속성 책임
 * - 상태 변경, 저장, 조회
 */
interface BrandPositionCommand {

    /**
     * ID 기반 단건 조회 (Write Context)
     */
    fun findById(id: Long): BrandPosition?

    /**
     * Brand + Position 조합 조회 (중복 검증 등)
     */
    fun findByBrandIdAndPositionId(
        brandId: Long,
        positionId: Long
    ): BrandPosition?

    /**
     * 저장
     */
    fun save(brandPosition: BrandPosition): BrandPosition
}
