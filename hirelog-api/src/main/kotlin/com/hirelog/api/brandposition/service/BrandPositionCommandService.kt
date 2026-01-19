package com.hirelog.api.brandposition.service

import com.hirelog.api.brandposition.domain.BrandPosition
import com.hirelog.api.brandposition.domain.BrandPositionSource
import com.hirelog.api.brandposition.repository.BrandPositionRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

/**
 * BrandPositionCommandService
 *
 * 역할:
 * - BrandPosition 생성 / 상태 변경 담당
 * - 비즈니스 로직은 도메인(BrandPosition)에 위임
 */
@Service
class BrandPositionCommandService(
    private val brandPositionRepository: BrandPositionRepository
) {

    /**
     * BrandPosition 생성
     *
     * 정책:
     * - (brandId, positionId) 조합은 유니크
     * - 최초 상태는 CANDIDATE
     */
    @Transactional
    fun create(
        brandId: Long,
        positionId: Long,
        displayName: String?,
        source: BrandPositionSource
    ): BrandPosition {

        require(
            !brandPositionRepository.existsByBrandIdAndPositionId(
                brandId,
                positionId
            )
        ) {
            "BrandPosition already exists. brandId=$brandId, positionId=$positionId"
        }

        val brandPosition = BrandPosition(
            brandId = brandId,
            positionId = positionId,
            displayName = displayName,
            source = source
        )

        return brandPositionRepository.save(brandPosition)
    }

    /**
     * 관리자 승인 처리
     *
     * - 상태를 ACTIVE로 전환
     * - 승인 정보 기록
     */
    @Transactional
    fun approve(
        brandPositionId: Long,
        adminId: Long
    ) {
        val brandPosition = getOrThrow(brandPositionId)
        brandPosition.approve(adminId)
        // save() 호출 불필요 (JPA dirty checking)
    }

    /**
     * BrandPosition 비활성화
     */
    @Transactional
    fun deactivate(brandPositionId: Long) {
        val brandPosition = getOrThrow(brandPositionId)
        brandPosition.deactivate()
    }

    /**
     * 공통 조회 유틸
     * - 존재하지 않으면 예외
     */
    private fun getOrThrow(id: Long): BrandPosition =
        brandPositionRepository.findById(id)
            .orElseThrow { IllegalArgumentException("BrandPosition not found: $id") }
}
