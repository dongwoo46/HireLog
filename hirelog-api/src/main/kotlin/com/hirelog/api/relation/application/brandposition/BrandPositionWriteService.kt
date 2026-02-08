package com.hirelog.api.relation.application.brandposition

import com.hirelog.api.relation.application.brandposition.port.BrandPositionCommand
import com.hirelog.api.relation.application.brandposition.port.BrandPositionQuery
import com.hirelog.api.relation.domain.model.BrandPosition
import com.hirelog.api.relation.domain.type.BrandPositionSource
import com.hirelog.api.common.exception.EntityAlreadyExistsException
import com.hirelog.api.common.exception.EntityNotFoundException
import com.hirelog.api.relation.domain.type.BrandPositionStatus
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
     * BrandPosition 확보
     *
     * 정책:
     * - (brandId, positionId) 기준 단일 BrandPosition 보장
     * - 존재하면 반환
     * - 없으면 CANDIDATE 상태로 신규 생성
     * - 동시성은 DB unique + 재조회로 해결
     */
    @Transactional
    fun getOrCreate(
        brandId: Long,
        positionId: Long,
        displayName: String,
        source: BrandPositionSource
    ): BrandPosition {

        brandPositionCommand.findByBrandIdAndPositionId(brandId, positionId)?.let {
            return it
        }

        val brandPosition = BrandPosition.create(
            brandId = brandId,
            positionId = positionId,
            displayName = displayName,
            source = source
        )

        return try {
            brandPositionCommand.save(brandPosition)
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            brandPositionCommand.findByBrandIdAndPositionId(brandId, positionId)
                ?: throw ex
        }
    }

    /**
     * 명시적 생성
     */
    @Transactional
    fun create(
        brandId: Long,
        positionId: Long,
        displayName: String,
        source: BrandPositionSource
    ): BrandPosition {

        if (brandPositionQuery.existsByBrandIdAndPositionId(brandId, positionId)) {
            throw EntityAlreadyExistsException(
                entityName = "BrandPosition",
                identifier = "brandId=$brandId, positionId=$positionId"
            )
        }

        return brandPositionCommand.save(
            BrandPosition.create(
                brandId = brandId,
                positionId = positionId,
                displayName = displayName,
                source = source
            )
        )
    }

    /**
     * 회사명 변경
     */
    @Transactional
    fun changeDisplayName(
        brandPositionId: Long,
        newDisplayName: String
    ) {
        val brandPosition = getRequired(brandPositionId)
        brandPosition.changeDisplayName(newDisplayName)
        brandPositionCommand.save(brandPosition)
    }


    /**
     * BrandPosition 상태 변경
     *
     * 정책:
     * - 상태 전이는 도메인에서 검증
     * - Service는 트랜잭션 경계 + 오케스트레이션만 담당
     */
    @Transactional
    fun changeStatus(
        brandPositionId: Long,
        newStatus: BrandPositionStatus,
        adminId: Long
    ) {
        val brandPosition = getRequired(brandPositionId)

        brandPosition.changeStatus(
            newStatus = newStatus,
            adminId = adminId
        )

        brandPositionCommand.save(brandPosition)
    }

    /**
     * 필수 Aggregate 로딩 (Write Context)
     */
    private fun getRequired(id: Long): BrandPosition =
        brandPositionCommand.findById(id)
            ?: throw EntityNotFoundException(
                entityName = "BrandPosition",
                identifier = id
            )
}
