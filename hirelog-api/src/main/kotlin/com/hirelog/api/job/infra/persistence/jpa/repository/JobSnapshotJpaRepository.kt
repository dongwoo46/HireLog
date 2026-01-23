package com.hirelog.api.job.infra.persistence.jpa.repository

import com.hirelog.api.job.domain.JobSnapshot
import org.springframework.data.jpa.repository.JpaRepository

/**
 * JobSnapshot JPA Repository
 *
 * 책임:
 * - JobSnapshot Entity에 대한 DB 접근
 * - JPA 메서드 네이밍을 통한 단순 조회 제공
 *
 * 주의:
 * - 비즈니스 의미는 Adapter / Application 계층에서 정의한다
 */
interface JobSnapshotJpaRepository : JpaRepository<JobSnapshot, Long> {

    /**
     * canonicalHash 기준 Snapshot 조회
     *
     * 사용 목적:
     * - Snapshot 중복 판정
     */
    fun findByContentHash(contentHash: String): JobSnapshot?

    /**
     * 브랜드 + 포지션 기준 최신 Snapshot 1건 조회
     *
     * 사용 목적:
     * - 기본 Snapshot 선택
     * - 요약/분석 대상 결정
     */
    fun findFirstByBrandIdAndPositionIdOrderByCreatedAtDesc(
        brandId: Long,
        positionId: Long
    ): JobSnapshot?

    /**
     * 브랜드 + 포지션 기준 Snapshot 전체 조회 (최신 순)
     *
     * 사용 목적:
     * - Snapshot 히스토리 조회
     */
    fun findAllByBrandIdAndPositionIdOrderByCreatedAtDesc(
        brandId: Long,
        positionId: Long
    ): List<JobSnapshot>

    /**
     * URL 기준 Snapshot Entity 조회
     *
     * Command 흐름 전용
     */
    fun findAllBySourceUrl(url: String): List<JobSnapshot>

}
