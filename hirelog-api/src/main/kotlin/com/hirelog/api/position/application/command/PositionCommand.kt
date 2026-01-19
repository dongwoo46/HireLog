package com.hirelog.api.position.application.command

import com.hirelog.api.position.domain.Position

/**
 * Position Write Port
 *
 * 책임:
 * - Position 변경/생성 계약 정의
 * - 저장소 구현을 알지 않는다
 */
interface PositionCommand {

    /**
     * Position 생성
     */
    fun create(
        name: String,
        normalizedName: String,
        description: String?
    ): Position

    /**
     * Position 활성화
     */
    fun activate(positionId: Long)

    /**
     * Position 비활성화
     */
    fun deprecate(positionId: Long)
}
