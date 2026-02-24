package com.hirelog.api.position.infra.persistence.jpa.repository

import com.hirelog.api.position.domain.Position
import com.hirelog.api.position.domain.PositionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
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
     * 정규화된 포지션명 기준 단건 조회 (Category JOIN FETCH)
     *
     * 역할:
     * - 외부 문자열 입력을 Position으로 매핑
     * - 신규 Position 생성 전 중복 확인
     *
     * JOIN FETCH 이유:
     * - 비동기 컨텍스트(CompletableFuture)에서 category 접근 시
     *   LazyInitializationException 방지
     */
    @Query("SELECT p FROM Position p JOIN FETCH p.category WHERE p.normalizedName = :normalizedName")
    fun findByNormalizedName(
        normalizedName: String
    ): Position?

    @Query("select p.name from Position p")
    fun findAllNames(): List<String>


}
