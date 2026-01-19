package com.hirelog.api.brand.application.query

import com.hirelog.api.brand.domain.Brand

/**
 * Brand Read Port
 *
 * 책임:
 * - Brand 조회 계약 정의
 * - 조회 기술(JPA 등)과 분리
 */
interface BrandQuery {

    /**
     * ID 기준 Brand 조회
     *
     * 없으면 null 반환
     */
    fun findById(brandId: Long): Brand?

    /**
     * normalizedName 기준 Brand 조회
     *
     * 중복 판단/정책 결정용
     */
    fun findByNormalizedName(normalizedName: String): Brand?
}
