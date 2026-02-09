package com.hirelog.api.job.application.snapshot.port

import com.hirelog.api.job.application.snapshot.view.JobSnapshotView

/**
 * JobSnapshot 조회 Port
 *
 * 책임:
 * - JobSnapshot 조회 유스케이스 정의
 *
 * 설계 원칙:
 * - Query Port는 Read Model(View)만 반환
 * - Entity를 반환하는 메서드는 Command Port에 위치
 */
interface JobSnapshotQuery {

    /**
     * Snapshot 단건 조회
     */
    fun getSnapshot(snapshotId: Long): JobSnapshotView?

    /**
     * canonicalHash 기준 Snapshot 조회
     *
     * 사용 목적:
     * - 입력 중복 판정
     * - 동일 JD 여부 확인
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
