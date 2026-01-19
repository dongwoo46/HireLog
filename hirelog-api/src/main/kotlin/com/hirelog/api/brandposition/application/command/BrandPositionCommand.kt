package com.hirelog.api.brandposition.application.command

import com.hirelog.api.brandposition.domain.BrandPosition
import com.hirelog.api.brandposition.domain.BrandPositionSource

/**
 * BrandPosition Command Port
 *
 * 역할:
 * - BrandPosition 생성/변경 Command 추상화
 * - Application은 구현 기술(JPA 등)을 모른다
 */
interface BrandPositionCommand {

    fun create(
        brandId: Long,
        positionId: Long,
        displayName: String?,
        source: BrandPositionSource
    ): BrandPosition

    fun existsByBrandIdAndPositionId(
        brandId: Long,
        positionId: Long
    ): Boolean
}
