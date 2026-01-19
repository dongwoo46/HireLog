package com.hirelog.api.brandposition.application.query

import com.hirelog.api.brandposition.domain.BrandPosition

/**
 * BrandPosition Query Port
 *
 * 역할:
 * - BrandPosition 조회 책임 추상화
 * - 표현(API 응답)과는 완전히 분리
 *
 * 원칙:
 * - 도메인 엔티티를 그대로 반환
 * - DTO 변환은 Presentation 계층에서 수행
 */
interface BrandPositionQuery {

    /**
     * 특정 브랜드에 속한 모든 BrandPosition 조회
     */
    fun findAllByBrandId(brandId: Long): List<BrandPosition>

    /**
     * 브랜드 + 포지션 조합으로 단일 조회
     */
    fun findByBrandIdAndPositionId(
        brandId: Long,
        positionId: Long
    ): BrandPosition?
}
