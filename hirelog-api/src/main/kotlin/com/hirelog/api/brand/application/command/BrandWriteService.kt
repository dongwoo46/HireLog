package com.hirelog.api.brand.application.command

import com.hirelog.api.brand.application.query.BrandQuery
import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.common.domain.VerificationStatus
import com.hirelog.api.common.exception.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Brand Write Application Service
 *
 * 책임:
 * - Brand 쓰기 유스케이스 전담
 * - 트랜잭션 경계 정의
 * - 도메인 상태 변경 트리거
 *
 * 주의:
 * - 조회 API 제공 ❌
 * - Facade 역할 ❌
 */
@Service
class BrandWriteService(
    private val brandCommand: BrandCommand,
    private val brandQuery: BrandQuery
) {

    /**
     * Brand 확보
     *
     * 정책:
     * - normalizedName 기준 단일 Brand 보장
     * - 존재하면 반환
     * - 없으면 생성 (ON CONFLICT DO NOTHING)
     * - JPA 세션 오염 없이 동시성 안전
     */
    @Transactional
    fun getOrCreate(
        name: String,
        normalizedName: String,
        companyId: Long?,
        source: BrandSource
    ): Brand {

        // 1. 빠른 조회
        brandQuery.findByNormalizedName(normalizedName)?.let {
            return it
        }

        // 2. INSERT ... ON CONFLICT DO NOTHING (동시성 안전)
        brandCommand.insertIgnoreDuplicate(
            name = name,
            normalizedName = normalizedName,
            companyId = companyId,
            verificationStatus = VerificationStatus.UNVERIFIED,
            source = source,
            isActive = true
        )

        // 3. 재조회 (ON CONFLICT로 insert 안 됐을 수 있음)
        return brandQuery.findByNormalizedName(normalizedName)
            ?: throw IllegalStateException("Brand insert failed unexpectedly: normalizedName=$normalizedName")
    }

    /**
     * 브랜드 검증 승인
     */
    @Transactional
    fun verify(brandId: Long) {
        val brand = getRequired(brandId)
        brand.verify()
    }

    /**
     * 브랜드 검증 거절
     */
    @Transactional
    fun reject(brandId: Long) {
        val brand = getRequired(brandId)
        brand.reject()
    }

    /**
     * 브랜드 비활성화
     */
    @Transactional
    fun deactivate(brandId: Long) {
        val brand = getRequired(brandId)
        brand.deactivate()
    }

    /**
     * 쓰기 유스케이스 전용 필수 조회
     */
    private fun getRequired(brandId: Long): Brand =
        brandQuery.findById(brandId)
            ?: throw EntityNotFoundException(
                entityName = "Brand",
                identifier = brandId
            )
}

