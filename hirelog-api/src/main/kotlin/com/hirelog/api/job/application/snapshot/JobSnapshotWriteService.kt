package com.hirelog.api.job.application.snapshot

import com.hirelog.api.common.exception.EntityAlreadyExistsException
import com.hirelog.api.job.application.snapshot.command.JobSnapshotCreateCommand
import com.hirelog.api.job.application.snapshot.port.JobSnapshotCommand
import com.hirelog.api.job.application.snapshot.port.JobSnapshotQuery
import com.hirelog.api.job.domain.JobSnapshot
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * JobSnapshot Write Application Service
 *
 * 책임:
 * - JD 원문 Snapshot 생성
 * - 분석 완료 후 Brand / Position 연결
 *
 * 설계 원칙:
 * - Snapshot은 "수집 로그"이므로 중복 여부와 무관하게 생성 시도
 * - 중복 판정 정책은 상위 정책(JdIntakePolicy)에서 결정
 * - 이 Service는 Domain 생성 + 상태 변경 흐름만 담당
 */
@Service
class JobSnapshotWriteService(
    private val snapshotCommand: JobSnapshotCommand,
    private val snapshotQuery: JobSnapshotQuery
) {

    /**
     * JD 원문 Snapshot 기록
     *
     * 정책:
     * - Snapshot 생성 자체는 항상 시도한다
     * - canonicalHash 유일성은 DB 제약조건으로 보장
     *
     * 예외:
     * - 동일 canonicalHash가 이미 존재할 경우 EntityAlreadyExistsException 발생
     */
    @Transactional
    fun record(command: JobSnapshotCreateCommand): Long {
        return try {
            val snapshot = JobSnapshot.create(
                sourceType = command.sourceType,
                sourceUrl = command.sourceUrl,
                rawText = command.rawText,
                contentHash = command.contentHash,
                openedDate = command.openedDate,
                closedDate = command.closedDate
            )

            snapshotCommand.record(snapshot)
        } catch (ex: DataIntegrityViolationException) {
            throw EntityAlreadyExistsException(
                entityName = "JobSnapshot",
                identifier = "canonicalHash",
                cause = ex
            )
        }
    }

    /**
     * 분석 완료 후 Brand / Position 연결
     *
     * 규칙:
     * - Snapshot 생성 이후 단 한 번만 호출 가능
     * - 상태 변경 검증은 Domain(Entity)에서 수행
     */
    @Transactional
    fun attachBrandAndPosition(
        snapshotId: Long,
        brandId: Long,
        positionId: Long
    ) {
        /**
         * Command 전용 Entity 로딩
         *
         * 주의:
         * - 조회(View) 목적이 아님
         * - 상태 변경을 위한 최소 로딩
         */
        val snapshot = snapshotQuery.loadSnapshot(snapshotId)

        /**
         * Domain 규칙에 따른 상태 변경
         *
         * 예:
         * - 이미 brandId/positionId가 설정되어 있으면 예외 발생
         */
        snapshot.attachAnalysisResult(
            brandId = brandId,
            positionId = positionId
        )

        /**
         * 변경된 상태 영속화
         */
        snapshotCommand.update(snapshot)
    }
}
