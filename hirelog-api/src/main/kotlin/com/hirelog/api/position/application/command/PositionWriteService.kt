package com.hirelog.api.position.application.command

import com.hirelog.api.position.domain.Position
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Position Write Application Service
 *
 * 책임:
 * - Position 변경 유스케이스 실행
 * - 트랜잭션 경계 정의
 *
 * 주의:
 * - 조회 판단 ❌
 * - 순수 Write만 담당
 */
@Service
class PositionWriteService(
    private val positionCommand: PositionCommand
) {

    /**
     * Position 생성
     */
    @Transactional
    fun create(
        name: String,
        normalizedName: String,
        description: String?
    ): Position =
        positionCommand.create(
            name = name,
            normalizedName = normalizedName,
            description = description
        )

    /**
     * Position 활성화
     */
    @Transactional
    fun activate(positionId: Long) {
        positionCommand.activate(positionId)
    }

    /**
     * Position 비활성화
     */
    @Transactional
    fun deprecate(positionId: Long) {
        positionCommand.deprecate(positionId)
    }
}
