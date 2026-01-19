package com.hirelog.api.position.application.command

import com.hirelog.api.position.domain.Position

/**
 * Position Command Port
 *
 * 책임:
 * - Position 영속화 추상화
 * - 저장 / 삭제만 담당
 *
 * 금지:
 * - 상태 변경 ❌
 * - 조회 ❌
 * - 비즈니스 판단 ❌
 */
interface PositionCommand {

    /**
     * Position 저장
     *
     * - 신규 생성
     * - 상태 변경 후 반영 (Dirty Checking)
     */
    fun save(position: Position): Position

    /**
     * Position 삭제 (필요 시)
     */
    fun delete(position: Position)
}
