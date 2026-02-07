package com.hirelog.api.relation.infra.persistence.jpa.repository

import com.hirelog.api.relation.domain.model.BrandPosition
import org.springframework.data.jpa.repository.JpaRepository

interface BrandPositionJpaRepository : JpaRepository<BrandPosition, Long> {

    /**
     * 특정 브랜드 + 포지션 조합이 이미 존재하는지 확인
     * (중복 생성 방지용)
     */
    fun existsByBrandIdAndPositionId(
        brandId: Long,
        positionId: Long
    ): Boolean

    /**
     * 브랜드 + 포지션 조합으로 단일 BrandPosition 조회
     */
    fun findByBrandIdAndPositionId(
        brandId: Long,
        positionId: Long
    ): BrandPosition?

}
