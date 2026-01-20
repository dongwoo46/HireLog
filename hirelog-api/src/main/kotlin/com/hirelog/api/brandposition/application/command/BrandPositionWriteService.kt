package com.hirelog.api.brandposition.service

import com.hirelog.api.brandposition.domain.BrandPosition
import com.hirelog.api.brandposition.domain.BrandPositionSource
import com.hirelog.api.brandposition.infra.persistence.jpa.repository.BrandPositionJpaRepository
import com.hirelog.api.common.exception.EntityAlreadyExistsException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * BrandPositionWriteService
 *
 * 책임:
 * - BrandPosition 쓰기 유스케이스 전담
 * - 트랜잭션 경계 관리
 * - 도메인 상태 변경 트리거
 *
 * 주의:
 * - 조회(Query) 책임 없음
 * - 오케스트레이션 없음
 */
@Service
class BrandPositionWriteService(
    private val brandPositionRepository: BrandPositionJpaRepository
) {

    /**
     * BrandPosition 생성
     *
     * 정책:
     * - (brandId, positionId) 조합은 유니크
     * - 초기 상태는 CANDIDATE
     */
    @Transactional
    fun create(
        brandId: Long,
        positionId: Long,
        displayName: String?,
        source: BrandPositionSource
    ): BrandPosition {

        // 빠른 실패 목적의 사전 체크 (동시성 보장 아님)
        if (brandPositionRepository.existsByBrandIdAndPositionId(brandId, positionId)) {
            throw EntityAlreadyExistsException(
                entityName = "BrandPosition",
                identifier = "brandId=$brandId, positionId=$positionId",
            )
        }

        val brandPosition = BrandPosition.create(
            brandId = brandId,
            positionId = positionId,
            displayName = displayName,
            source = source
        )

        return try {
            brandPositionRepository.save(brandPosition)
        } catch (ex: DataIntegrityViolationException) {
            // 동시성 상황에서 발생한 unique constraint 위반을
            // 비즈니스 의미의 예외로 변환
            throw EntityAlreadyExistsException(
                entityName = "BrandPosition",
                identifier = "brandId=$brandId, positionId=$positionId",
                cause = ex
            )
        }
    }

    /**
     * 관리자 승인
     */
    @Transactional
    fun approve(
        brandPositionId: Long,
        adminId: Long
    ) {
        val brandPosition = getRequiredById(brandPositionId)
        brandPosition.approve(adminId)
    }

    /**
     * 비활성화
     */
    @Transactional
    fun deactivate(brandPositionId: Long) {
        val brandPosition = getRequiredById(brandPositionId)
        brandPosition.deactivate()
    }

    /**
     * 쓰기 유스케이스 전용 Aggregate 조회
     *
     * 역할:
     * - 반드시 존재해야 하는 BrandPosition 로드
     * - 없으면 즉시 실패
     */
    private fun getRequiredById(id: Long): BrandPosition =
        requireNotNull(brandPositionRepository.findByIdOrNull(id)) {
            "BrandPosition not found. id=$id"
        }
}
