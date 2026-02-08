package com.hirelog.api.position.application.port

import com.hirelog.api.position.domain.Position

/**
 * Position Command Port
 *
 * 책임:
 * - Position 쓰기 유스케이스를 위한 영속화 계약
 * - 상태 변경을 위한 Entity 로딩
 *
 * 정책:
 * - 물리 삭제 ❌
 * - 조회는 수정 목적일 때만 허용
 */
interface PositionCommand {

    /**
     * Position 저장
     */
    fun save(position: Position): Position

    /**
     * ID로 Position 조회 (수정/상태변경 목적)
     */
    fun findById(id: Long): Position?

    /**
     * normalizedName으로 Position 조회
     *
     * 용도:
     * - 생성 시 중복 체크
     * - Alias 매핑 시 기준 확인
     */
    fun findByNormalizedName(normalizedName: String): Position?
}
