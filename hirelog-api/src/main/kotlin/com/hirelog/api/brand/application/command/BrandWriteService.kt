package com.hirelog.api.brand.application.command

import com.hirelog.api.brand.application.query.BrandQuery
import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.common.exception.EntityAlreadyExistsException
import com.hirelog.api.common.exception.EntityNotFoundException
import org.springframework.dao.DataIntegrityViolationException
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
     * - 없으면 생성
     * - 동시성 충돌 시 DB 예외 → 재조회
     */
    @Transactional
    fun getOrCreate(
        name: String,
        normalizedName: String,
        companyId: Long?,
        source: BrandSource
    ): Brand {

        // 1. 선 조회
        // 중복 선검증
        if (brandQuery.existsByNormalizedName(normalizedName)) {
            throw EntityAlreadyExistsException(
                entityName = "Brand",
                identifier = normalizedName
            )
        }

        // 2. 생성 시도
        return try {
            brandCommand.save(
                Brand.create(
                    name = name,
                    normalizedName = normalizedName,
                    companyId = companyId,
                    source = source,
                )
            )
        } catch (e: DataIntegrityViolationException) {
            // 3. 동시성 충돌 → 이미 생성됨
            brandCommand.findByNormalizedName(normalizedName)
                ?: throw e
        }
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
        brandCommand.findById(brandId)
            ?: throw EntityNotFoundException(
                entityName = "Brand",
                identifier = brandId
            )
}

