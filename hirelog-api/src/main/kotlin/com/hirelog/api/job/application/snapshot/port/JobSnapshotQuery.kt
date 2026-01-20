package com.hirelog.api.job.application.snapshot.port

import com.hirelog.api.job.application.snapshot.view.JobSnapshotView

/**
 * JobSnapshot 조회 Port
 *
 * 책임:
 * - Snapshot 조회 유스케이스 정의
 * - 반환 타입은 Read Model(View)로 고정
 */
interface JobSnapshotQuery {

    /**
     * Snapshot 단건 조회
     */
    fun getSnapshot(snapshotId: Long): JobSnapshotView?

    /**
     * canonicalHash 기준 Snapshot 조회
     *
     * 용도:
     * - 중복 판정
     */
    fun getSnapshotByCanonicalHash(
        canonicalHash: String
    ): JobSnapshotView?

    /**
     * 브랜드 + 포지션 기준 Snapshot 목록 조회
     */
    fun listSnapshotsForPosition(
        brandId: Long,
        positionId: Long
    ): List<JobSnapshotView>

    /**
     * 브랜드 + 포지션 기준 최신 Snapshot 조회
     */
    fun getLatestSnapshotForPosition(
        brandId: Long,
        positionId: Long
    ): JobSnapshotView?
}
