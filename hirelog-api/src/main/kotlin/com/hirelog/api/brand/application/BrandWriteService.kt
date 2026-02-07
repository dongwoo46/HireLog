package com.hirelog.api.brand.application

import com.hirelog.api.brand.application.port.BrandCommand
import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.common.exception.EntityNotFoundException
import com.hirelog.api.common.utils.Normalizer
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
 * 비책임:
 * - 조회 API 제공 ❌
 * - Facade 역할 ❌
 */
@Service
class BrandWriteService(
    private val brandCommand: BrandCommand
) {

    /**
     * Brand 확보 (Write 전용 유스케이스)
     *
     * 정책:
     * - normalizedName 기준 단일 Brand 보장
     * - 존재하면 반환
     * - 없으면 생성
     * - 동시성 충돌 시 DB 제약으로 보정
     *
     * 주의:
     * - 조회처럼 보이지만 "쓰기 유스케이스의 일부"이다
     */
    @Transactional
    fun getOrCreate(
        name: String,
        companyId: Long?,
        source: BrandSource
    ): Brand {
        val normalizedName = Normalizer.normalizeBrand(name)

        // 1. Write 선행 조회 (이미 존재하면 그대로 사용)
        brandCommand.findByNormalizedName(normalizedName)
            ?.let { return it }

        // 2. 신규 생성 시도
        return try {
            brandCommand.save(
                Brand.create(
                    name = name,
                    companyId = companyId,
                    source = source
                )
            )
        } catch (e: DataIntegrityViolationException) {
            // 3. 동시성 충돌 → 이미 생성된 Brand 재조회
            brandCommand.findByNormalizedName(normalizedName)
                ?: throw e
        }
    }

    @Transactional
    fun create(
        name: String,
        companyId: Long?,
    ): Brand {

        val normalizedName = Normalizer.normalizeBrand(name)

        require(
            brandCommand.findByNormalizedName(normalizedName) == null
        ) {
            "Brand already exists (normalizedName=$normalizedName)"
        }

        return brandCommand.save(
            Brand.createByAdmin(
                name = name,
                companyId = companyId,
            )
        )
    }


    /**
     * 브랜드 검증 승인
     *
     * 정책:
     * - 관리자 액션
     * - Brand 도메인 규칙에 따라 상태 전이
     */
    @Transactional
    fun verify(brandId: Long) {
        val brand = getRequiredForWrite(brandId)
        brand.verify()
        brandCommand.save(brand)
    }

    /**
     * 브랜드 검증 거절
     *
     * 정책:
     * - 관리자 액션
     * - 거절 이후 상태는 도메인이 결정
     */
    @Transactional
    fun reject(brandId: Long) {
        val brand = getRequiredForWrite(brandId)
        brand.reject()
        brandCommand.save(brand)
    }

    /**
     * 브랜드 활성화
     */
    @Transactional
    fun activate(brandId: Long) {
        val brand = getRequiredForWrite(brandId)
        brand.activate()
        brandCommand.save(brand)
    }

    /**
     * 브랜드 비활성화
     *
     * 정책:
     * - 소프트 삭제 개념
     * - 재활성화 여부는 도메인 정책에 따름
     */
    @Transactional
    fun deactivate(brandId: Long) {
        val brand = getRequiredForWrite(brandId)
        brand.deactivate()
        brandCommand.save(brand)
    }

    /**
     * Write 유스케이스 전용 필수 조회
     *
     * - ReadService에서 사용 ❌
     * - 존재하지 않으면 즉시 실패
     */
    private fun getRequiredForWrite(brandId: Long): Brand =
        brandCommand.findById(brandId)
            ?: throw EntityNotFoundException(
                entityName = "Brand",
                identifier = brandId
            )
}
