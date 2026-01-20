package com.hirelog.api.job.application.snapshot.port
import com.hirelog.api.job.domain.JobSnapshot

interface JobSnapshotCommand {

    /**
     * JobSnapshot 기록
     *
     * 책임:
     * - Snapshot 영속화
     * - 생성 규칙은 Domain에서 보장
     *
     * 반환:
     * - 생성된 Snapshot 식별자
     */
    fun record(
        snapshot: JobSnapshot
    ): Long
}
