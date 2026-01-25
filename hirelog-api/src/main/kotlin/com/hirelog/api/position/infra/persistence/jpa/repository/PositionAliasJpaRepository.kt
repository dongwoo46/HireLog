package com.hirelog.api.position.infra.persistence.jpa.repository

import com.hirelog.api.position.domain.PositionAlias
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * PositionAliasJpaRepository
 *
 * 책임:
 * - PositionAlias 엔티티의 영속성 관리
 * - Load / Command Port 구현을 위한 최소 기능 제공
 *
 * 주의:
 * - View 반환 ❌
 * - 목록 조회 ❌
 * - 상태 필터링 ❌
 * - 비즈니스 로직 ❌
 */
@Repository
interface PositionAliasJpaRepository : JpaRepository<PositionAlias, Long> {

    /**
     * 정규화된 Alias 명칭 기준 단건 조회
     *
     * 용도:
     * - Alias 중복 생성 방지
     * - 기존 Alias 존재 여부 확인
     */
    fun findByNormalizedAliasName(
        normalizedAliasName: String
    ): PositionAlias?
}
