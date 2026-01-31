package com.hirelog.api.brand.infra.persistence.jpa

import com.hirelog.api.brand.application.command.BrandCommand
import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.common.domain.VerificationStatus
import org.springframework.stereotype.Component

/**
 * Brand JPA Command Adapter
 *
 * 책임:
 * - BrandCommand Port의 JPA 구현
 * - Entity 생성/상태 변경을 영속화
 */
@Component
class BrandJpaCommand(
    private val brandRepository: BrandJpaRepository
) : BrandCommand {

    /**
     * Brand 저장
     *
     * - 신규/수정 공통
     * - Dirty Checking 기반
     */
    override fun save(brand: Brand): Brand {
        return brandRepository.save(brand)
    }

}

