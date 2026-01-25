package com.hirelog.api.position.application.port

import com.hirelog.api.position.application.query.PositionAliasView

/**
 * PositionAliasQuery
 *
 * 책임:
 * - PositionAlias 조회 (Read 전용)
 * - UI / API / 관리자 화면 대응
 */
interface PositionAliasQuery {

    /**
     * 활성 Alias 단건 조회
     *
     * 사용 예:
     * - 외부 입력 문자열 매핑
     */
    fun findActiveByNormalizedAlias(
        normalizedAliasName: String
    ): PositionAliasView?

    /**
     * 특정 Position에 속한 Alias 목록 조회
     *
     * 사용 예:
     * - 관리자 관리 화면
     */
    fun listByPosition(
        positionId: Long
    ): List<PositionAliasView>
}
