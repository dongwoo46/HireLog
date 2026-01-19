package com.hirelog.api.brand.application.command

import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.brand.domain.BrandSource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Brand Write Application Service
 *
 * 책임:
 * - Brand 변경 유스케이스 실행
 * - 트랜잭션 경계 정의
 *
 * 주의:
 * - 조회 판단(getOrCreate 등) ❌
 * - 순수 Write만 담당
 */
@Service
class BrandWriteService(
    private val brandCommand: BrandCommand
) {

    /**
     * Brand 생성
     */
    @Transactional
    fun create(
        name: String,
        normalizedName: String,
        companyId: Long?,
        source: BrandSource
    ): Brand {
        return brandCommand.create(
            name = name,
            normalizedName = normalizedName,
            companyId = companyId,
            source = source
        )
    }

    /**
     * 브랜드 검증 승인
     */
    @Transactional
    fun verify(brandId: Long) {
        brandCommand.verify(brandId)
    }

    /**
     * 브랜드 검증 거절
     */
    @Transactional
    fun reject(brandId: Long) {
        brandCommand.reject(brandId)
    }

    /**
     * 브랜드 비활성화
     */
    @Transactional
    fun deactivate(brandId: Long) {
        brandCommand.deactivate(brandId)
    }
}
