package com.hirelog.api.relation.infra.persistence.jpa.adapter

import com.hirelog.api.relation.application.brandposition.port.BrandPositionCommand
import com.hirelog.api.relation.domain.model.BrandPosition
import com.hirelog.api.relation.infra.persistence.jpa.repository.BrandPositionJpaRepository
import com.hirelog.api.common.exception.EntityAlreadyExistsException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

/**
 * BrandPosition JPA Command Adapter
 *
 * 역할:
 * - BrandPositionCommand Port의 JPA 구현체
 * - 저장 책임만 수행
 *
 * 원칙:
 * - 도메인 생성 ❌
 * - 조회 ❌
 */
@Component
class BrandPositionJpaCommand(
    private val repository: BrandPositionJpaRepository
) : BrandPositionCommand {

    override fun save(brandPosition: BrandPosition): BrandPosition =
        try {
            repository.save(brandPosition)
        } catch (ex: DataIntegrityViolationException) {
            // (brandId, positionId) 유니크 제약 위반 → 비즈니스 예외로 변환
            throw EntityAlreadyExistsException(
                entityName = "BrandPosition",
                identifier = "brandId=${brandPosition.brandId}, positionId=${brandPosition.positionId}",
                cause = ex
            )
        }
}
