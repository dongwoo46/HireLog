package com.hirelog.api.brandposition.infra.persistence.jpa.adapter

import com.hirelog.api.brandposition.application.command.BrandPositionCommand
import com.hirelog.api.brandposition.domain.BrandPosition
import com.hirelog.api.brandposition.domain.BrandPositionSource
import com.hirelog.api.brandposition.infra.persistence.jpa.repository.BrandPositionJpaRepository
import org.springframework.stereotype.Component

/**
 * BrandPosition JPA Command Adapter
 *
 * 역할:
 * - BrandPositionCommand Port의 JPA 구현체
 *
 * 중요:
 * - @Component 로 등록되어야 Spring Bean으로 인식된다
 * - 이 Bean이 존재해야 BrandPositionCommand 주입이 가능하다
 */
@Component
class BrandPositionJpaCommand(
    private val repository: BrandPositionJpaRepository
) : BrandPositionCommand {

    override fun create(
        brandId: Long,
        positionId: Long,
        displayName: String?,
        source: BrandPositionSource
    ): BrandPosition {

        // 도메인 생성 규칙은 Entity factory에 위임
        val entity = BrandPosition.create(
            brandId = brandId,
            positionId = positionId,
            displayName = displayName,
            source = source
        )

        return repository.save(entity)
    }

    override fun existsByBrandIdAndPositionId(
        brandId: Long,
        positionId: Long
    ): Boolean =
        repository.existsByBrandIdAndPositionId(brandId, positionId)
}
