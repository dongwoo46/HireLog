package com.hirelog.api.job.infra.persistence.jpa.repository

import com.hirelog.api.job.domain.JobSnapshot
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

/**
 * JobSnapshot JPA Repository
 *
 * 책임:
 * - JobSnapshot Entity에 대한 DB 접근
 * - 쿼리 구현은 JPA 메서드 네이밍 규칙 사용
 */
interface JobSnapshotJpaRepository : JpaRepository<JobSnapshot, Long> {

    /**
     * 콘텐츠 해시 기준 Snapshot 조회
     */
    fun findByContentHash(contentHash: String): JobSnapshot?

    /**
     * 브랜드 + 포지션 기준 최신 Snapshot 조회
     */
    fun findByBrandIdAndPositionIdOrderByCreatedAtDesc(
        brandId: Long,
        positionId: Long,
        pageable: Pageable
    ): List<JobSnapshot>

    /**
     * 브랜드 + 포지션 기준 전체 Snapshot 조회
     */
    fun findAllByBrandIdAndPositionIdOrderByCreatedAtDesc(
        brandId: Long,
        positionId: Long
    ): List<JobSnapshot>
}
