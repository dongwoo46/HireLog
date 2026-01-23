package com.hirelog.api.job.application.snapshot.port

import com.hirelog.api.job.domain.JobSnapshot

/**
 * JobSnapshot Command Port
 *
 * 책임:
 * - JobSnapshot 생성
 * - 분석 결과 반영을 위한 상태 변경
 *
 * 주의:
 * - 조회 책임 ❌
 */
interface JobSnapshotCommand {

    /**
     * Snapshot 최초 기록
     *
     * 사용 시점:
     * - JD 수집 직후
     */
    fun record(
        snapshot: JobSnapshot
    ): Long

    /**
     * 분석 결과 반영
     *
     * 규칙:
     * - brandId / positionId는 단 한 번만 설정됨
     */
    fun update(
        snapshot: JobSnapshot
    )
}
