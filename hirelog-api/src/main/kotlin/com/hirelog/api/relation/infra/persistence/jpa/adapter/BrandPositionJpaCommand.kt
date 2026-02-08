package com.hirelog.api.relation.infra.persistence.jpa.adapter

import com.hirelog.api.relation.application.brandposition.port.BrandPositionCommand
import com.hirelog.api.relation.domain.model.BrandPosition
import com.hirelog.api.relation.infra.persistence.jpa.repository.BrandPositionJpaRepository
import org.springframework.stereotype.Component

/**
 * BrandPositionCommandJpaAdapter
 *
 * 책임:
 * - BrandPosition 도메인 엔티티 조회 및 저장 (Write Context)
 *
 * 설계 원칙:
 * - 엔티티 반환 ⭕
 * - 조회는 상태 변경/검증 목적만 허용
 * - 트랜잭션은 Application Service에서 관리
 */
@Component
class BrandPositionJpaCommand(
    private val repository: BrandPositionJpaRepository
) : BrandPositionCommand {

    /**
     * ID 기반 단건 조회 (Write Context)
     */
    override fun findById(id: Long): BrandPosition? {
        return repository.findById(id).orElse(null)
    }

    /**
     * Brand + Position 조합 조회
     *
     * 사용 목적:
     * - 중복 생성 방지
     * - 상태 변경 대상 조회
     */
    override fun findByBrandIdAndPositionId(
        brandId: Long,
        positionId: Long
    ): BrandPosition? {
        return repository.findByBrandIdAndPositionId(
            brandId = brandId,
            positionId = positionId
        )
    }

    /**
     * BrandPosition 저장
     */
    override fun save(brandPosition: BrandPosition): BrandPosition {
        return repository.save(brandPosition)
    }
}
