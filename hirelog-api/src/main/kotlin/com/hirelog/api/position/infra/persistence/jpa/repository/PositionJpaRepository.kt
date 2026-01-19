package com.hirelog.api.position.infra.persistence.jpa.repository


import com.hirelog.api.position.domain.Position
import com.hirelog.api.position.domain.PositionStatus
import org.springframework.data.jpa.repository.JpaRepository

interface PositionJpaRepository : JpaRepository<Position, Long> {

    /**
     * normalizedName 기준 단건 조회
     *
     * 역할:
     * - 중복 검사
     * - 외부 입력 매핑
     */
    fun findByNormalizedName(
        normalizedName: String
    ): Position?

    /**
     * normalizedName 기준 존재 여부 확인
     *
     * 역할:
     * - 신규 Position 생성 시 중복 방지
     */
    fun existsByNormalizedName(
        normalizedName: String
    ): Boolean

    /**
     * 상태별 Position 목록 조회
     *
     * 역할:
     * - ACTIVE 포지션 목록 조회
     * - UI / 검색 / 추천용
     */
    fun findAllByStatus(
        status: PositionStatus
    ): List<Position>
}
