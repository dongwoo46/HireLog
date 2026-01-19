package com.hirelog.api.position.application.facade

import com.hirelog.api.position.application.command.PositionWriteService
import com.hirelog.api.position.application.query.PositionQuery
import com.hirelog.api.position.domain.Position
import org.springframework.stereotype.Service

/**
 * Position Facade Service
 *
 * 책임:
 * - Position 관련 정책 결정
 * - 조회/생성/상태 변경 오케스트레이션
 */
@Service
class PositionFacadeService(
    private val positionQuery: PositionQuery,
    private val positionWriteService: PositionWriteService
) {

    /**
     * normalizedName 기준 Position 확보
     *
     * 정책:
     * - 존재하면 반환
     * - 없으면 생성
     */
    fun getOrCreate(
        name: String,
        normalizedName: String,
        description: String?
    ): Position {
        return positionQuery.findByNormalizedName(normalizedName)
            ?: positionWriteService.create(
                name = name,
                normalizedName = normalizedName,
                description = description
            )
    }

    /**
     * 필수 Position 조회
     */
    fun getRequiredById(positionId: Long): Position =
        requireNotNull(positionQuery.findById(positionId)) {
            "Position not found: $positionId"
        }

    /**
     * 활성 Position 목록 조회
     */
    fun findActive(): List<Position> =
        positionQuery.findActive()
}
