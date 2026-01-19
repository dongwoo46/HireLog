package com.hirelog.api.brandposition.service

import com.hirelog.api.brandposition.domain.BrandPosition
import com.hirelog.api.brandposition.repository.BrandPositionRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * BrandPositionQueryService
 *
 * 역할:
 * - BrandPosition 조회 전용 서비스
 * - 상태 변경 / 생성 로직 절대 없음
 */
@Service
class BrandPositionQueryService(
    private val brandPositionRepository: BrandPositionRepository
) {

    /**
     * ID 기반 단일 조회
     */
    @Transactional(readOnly = true)
    fun findById(id: Long): BrandPosition =
        brandPositionRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("BrandPosition not found: $id")

    /**
     * 특정 브랜드에 속한 모든 포지션 조회
     */
    @Transactional(readOnly = true)
    fun findByBrandId(brandId: Long): List<BrandPosition> =
        brandPositionRepository.findAllByBrandId(brandId)

    /**
     * 브랜드 + 포지션 조합으로 조회
     * (없으면 null)
     */
    @Transactional(readOnly = true)
    fun findByBrandAndPosition(
        brandId: Long,
        positionId: Long
    ): BrandPosition? =
        brandPositionRepository.findByBrandIdAndPositionId(
            brandId,
            positionId
        )
}
