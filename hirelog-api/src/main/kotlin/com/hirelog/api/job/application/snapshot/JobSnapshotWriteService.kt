package com.hirelog.api.job.application.snapshot

import com.hirelog.api.common.exception.EntityAlreadyExistsException
import com.hirelog.api.job.application.snapshot.port.JobSnapshotCommand
import com.hirelog.api.job.domain.JobSnapshot
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * JobSnapshot 기록 유스케이스 Service
 *
 * 책임:
 * - JobSnapshot 결과를 영속화한다
 * - canonicalHash 유일성은 DB 제약으로 보장한다
 *
 * 설계 원칙:
 * - 이 단계에서는 중복을 판단하지 않는다
 * - 정합성 위반은 DB 예외를 도메인 예외로 변환한다
 */
@Service
class JobSnapshotWriteService(
    private val snapshotCommand: JobSnapshotCommand
) {

    /**
     * JobSnapshot 기록
     *
     * 정책:
     * - canonicalHash는 전역적으로 유일해야 한다
     * - DB unique constraint 위반 시 중복으로 간주한다
     *
     * @return 생성된 JobSnapshot ID
     */
    @Transactional
    fun record(snapshot: JobSnapshot): Long {
        return try {
            snapshotCommand.record(snapshot)
        } catch (ex: DataIntegrityViolationException) {
            throw EntityAlreadyExistsException(
                entityName = "JobSnapshot",
                identifier = "canonicalHash=${snapshot.contentHash}",
                cause = ex
            )
        }
    }
}
