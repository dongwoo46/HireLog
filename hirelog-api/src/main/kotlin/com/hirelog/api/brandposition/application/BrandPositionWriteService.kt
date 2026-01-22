package com.hirelog.api.brandposition.application.command

import com.hirelog.api.brandposition.application.query.BrandPositionQuery
import com.hirelog.api.brandposition.domain.BrandPosition
import com.hirelog.api.brandposition.domain.BrandPositionSource
import com.hirelog.api.common.exception.EntityAlreadyExistsException
import com.hirelog.api.common.exception.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * BrandPosition Write Application Service
 *
 * 책임:
 * - BrandPosition 쓰기 유스케이스 오케스트레이션
 * - 트랜잭션 경계 정의
 *
 * 비책임:
 * - 저장소 구현 ❌
 * - 조회 전략 ❌
 */
@Service
class BrandPositionWriteService(
    private val brandPositionCommand: BrandPositionCommand,
    private val brandPositionQuery: BrandPositionQuery
) {

    /**
     * BrandPosition 생성
     *
     * 정책:
     * - (brandId, positionId)는 논리적으로 유니크
     * - 초기 상태는 CANDIDATE
     */
    @Transactional
    fun create(
        brandId: Long,
        positionId: Long,
        displayName: String?,
        source: BrandPositionSource
    ): BrandPosition {

        // 의미적 중복 체크 (UX 목적, 동시성 보장 아님)
        brandPositionQuery.findByBrandIdAndPositionId(brandId, positionId)?.let {
            throw EntityAlreadyExistsException(
                entityName = "BrandPosition",
                identifier = "brandId=$brandId, positionId=$positionId"
            )
        }

        val brandPosition = BrandPosition.create(
            brandId = brandId,
            positionId = positionId,
            displayName = displayName,
            source = source
        )

        // 실제 동시성 보장은 DB Unique + Adapter 예외 변환
        return brandPositionCommand.save(brandPosition)
    }

    /**
     * 관리자 승인
     */
    @Transactional
    fun approve(
        brandPositionId: Long,
        adminId: Long
    ) {
        val brandPosition = getRequired(brandPositionId)
        brandPosition.approve(adminId)
    }

    /**
     * 비활성화
     */
    @Transactional
    fun deactivate(brandPositionId: Long) {
        val brandPosition = getRequired(brandPositionId)
        brandPosition.deactivate()
    }

    /**
     * 반드시 존재해야 하는 Aggregate 로드
     */
    private fun getRequired(id: Long): BrandPosition =
        brandPositionQuery.findById(id)
            ?: throw EntityNotFoundException(
                entityName = "BrandPosition",
                identifier = id
            )
}
