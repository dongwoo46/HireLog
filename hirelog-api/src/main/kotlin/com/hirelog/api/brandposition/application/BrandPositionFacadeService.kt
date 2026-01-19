package com.hirelog.api.brandposition.facade

import com.hirelog.api.brandposition.domain.BrandPosition
import com.hirelog.api.brandposition.domain.BrandPositionSource
import com.hirelog.api.brandposition.service.BrandPositionWriteService
import org.springframework.stereotype.Service

/**
 * BrandPositionFacadeService
 *
 * 책임:
 * - BrandPosition 쓰기 유스케이스의 단일 진입점
 * - WriteService 오케스트레이션
 *
 * 설계 원칙:
 * - 조회(Query) 책임 없음
 * - 트랜잭션 없음
 * - 비즈니스 로직 없음
 * - 흐름 제어만 수행
 */
@Service
class BrandPositionFacadeService(
    private val writeService: BrandPositionWriteService
) {

    /**
     * BrandPosition 생성
     */
    fun create(
        brandId: Long,
        positionId: Long,
        displayName: String?,
        source: BrandPositionSource
    ): BrandPosition =
        writeService.create(
            brandId = brandId,
            positionId = positionId,
            displayName = displayName,
            source = source
        )

    /**
     * BrandPosition 관리자 승인
     */
    fun approve(
        brandPositionId: Long,
        adminId: Long
    ) {
        writeService.approve(
            brandPositionId = brandPositionId,
            adminId = adminId
        )
    }

    /**
     * BrandPosition 비활성화
     */
    fun deactivate(brandPositionId: Long) {
        writeService.deactivate(brandPositionId)
    }
}
