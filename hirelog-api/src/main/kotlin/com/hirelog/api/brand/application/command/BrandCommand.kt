package com.hirelog.api.brand.application.command

import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.brand.domain.BrandSource

/**
 * Brand Write Port
 *
 * 책임:
 * - Brand 생성/상태 변경 계약 정의
 * - 저장소 구현을 알지 않는다
 */
interface BrandCommand {

    /**
     * Brand 생성
     */
    fun create(
        name: String,
        normalizedName: String,
        companyId: Long?,
        source: BrandSource
    ): Brand

    /**
     * 브랜드 검증 승인
     */
    fun verify(brandId: Long)

    /**
     * 브랜드 검증 거절
     */
    fun reject(brandId: Long)

    /**
     * 브랜드 비활성화
     */
    fun deactivate(brandId: Long)
}
