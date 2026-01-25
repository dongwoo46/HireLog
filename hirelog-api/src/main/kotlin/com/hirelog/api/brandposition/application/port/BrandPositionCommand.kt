package com.hirelog.api.brandposition.application.port

import com.hirelog.api.brandposition.domain.BrandPosition

/**
 * BrandPosition Command Port
 *
 * 역할:
 * - BrandPosition 영속화 책임
 * - 저장소 구현(JPA 등)은 숨긴다
 */
interface BrandPositionCommand {

    fun save(brandPosition: BrandPosition): BrandPosition
}
