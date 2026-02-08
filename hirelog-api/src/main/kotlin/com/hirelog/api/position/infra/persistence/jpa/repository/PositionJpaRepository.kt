package com.hirelog.api.position.infra.persistence.jpa.repository

import com.hirelog.api.position.domain.Position
import com.hirelog.api.position.domain.PositionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * PositionJpaRepository
 *
 * 역할:
 * - Position 엔티티의 영속성 관리
 * - 쓰기(Command) 및 단순 Entity 조회 전용
 *
 * 책임 범위:
 * - 중복 검증
 * - 상태 기반 단순 조회
 */
@Repository
interface PositionJpaRepository : JpaRepository<Position, Long> {

    /**
     * 정규화된 포지션명 기준 단건 조회
     *
     * 역할:
     * - 외부 문자열 입력을 Position으로 매핑
     * - 신규 Position 생성 전 중복 확인
     */
    fun findByNormalizedName(
        normalizedName: String
    ): Position?


}
