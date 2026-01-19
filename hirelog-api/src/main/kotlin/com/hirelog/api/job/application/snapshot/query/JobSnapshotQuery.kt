package com.hirelog.api.job.application.snapshot.query

import com.hirelog.api.job.domain.JobSnapshot

/**
 * JobSnapshot 조회 Port
 *
 * 책임:
 * - JobSnapshot 조회 유스케이스 정의
 * - 조회 결과는 Domain(Entity) 기준
 */
interface JobSnapshotQuery {

    /**
     * 콘텐츠 해시 기준 조회
     */
    fun findByContentHash(contentHash: String): JobSnapshot?

    /**
     * ID 기준 조회
     */
    fun findById(snapshotId: Long): JobSnapshot?

    /**
     * 브랜드 + 포지션 기준 최신 JobSnapshot 조회
     *
     * 최신(latest)의 정의:
     * - createdAt DESC 기준
     * - 가장 최근에 생성된 Snapshot
     */
    fun findLatestByBrandAndPosition(
        brandId: Long,
        positionId: Long
    ): JobSnapshot?

    /**
     * 브랜드 + 포지션 기준 전체 Snapshot 조회
     */
    fun findAllByBrandAndPosition(
        brandId: Long,
        positionId: Long
    ): List<JobSnapshot>
}
