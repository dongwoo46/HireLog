package com.hirelog.api.position.application.port

import com.hirelog.api.position.domain.PositionAlias

/**
 * PositionAliasCommand
 *
 * 책임:
 * - PositionAlias 영속화 추상화
 *
 * 금지:
 * - 상태 변경 ❌
 * - 비즈니스 판단 ❌
 * - 조회 ❌
 */
interface PositionAliasCommand {

    /**
     * PositionAlias 저장
     *
     * - 신규 생성
     * - 상태 변경 후 반영 (Dirty Checking)
     */
    fun save(alias: PositionAlias): PositionAlias
}
