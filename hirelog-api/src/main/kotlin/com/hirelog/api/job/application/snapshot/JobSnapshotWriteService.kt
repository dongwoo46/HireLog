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
 * - JD Snapshot 생성
 * - 분석 완료 후 Brand / Position 연결
 *
 * 설계 원칙:
 * - Snapshot은 "수집 로그" 성격의 Aggregate
 * - 중복 여부 판단은 상위 정책(JdIntakePolicy)에서 수행
 * - 본 Service는 Domain 생성 및 상태 변경만 담당
 */
@Service
class JobSnapshotWriteService(
    private val snapshotCommand: JobSnapshotCommand,
    private val snapshotQuery: JobSnapshotQuery
) {

    /**
     * JD Snapshot 기록
     *
     * 정책:
     * - Snapshot 생성 시도는 항상 수행
     * - contentHash 유일성은 DB 제약조건으로 보장
     *
     * 예외:
     * - 동일 contentHash 존재 시 EntityAlreadyExistsException 발생
     */
    @Transactional
    fun record(command: JobSnapshotCreateCommand): Long {
        return try {

            val snapshot = JobSnapshot.create(
                sourceType = command.sourceType,
                sourceUrl = command.sourceUrl,
                canonicalSections = command.canonicalMap,
                recruitmentPeriodType = command.recruitmentPeriodType,
                openedDate = command.openedDate,
                closedDate = command.closedDate,
                canonicalHash = command.canonicalHash,
                simHash = command.simHash,
                coreText = command.coreText
            )

            snapshotCommand.record(snapshot)

        } catch (ex: DataIntegrityViolationException) {
            throw EntityAlreadyExistsException(
                entityName = "JobSnapshot",
                identifier = "contentHash",
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
         * 상태 변경을 위한 Command 전용 로딩
         *
         * 주의:
         * - 조회(View) 목적 아님
         * - Aggregate 상태 변경을 위한 최소 로딩
         */
        val snapshot = snapshotQuery.loadSnapshot(snapshotId)

        /**
         * Domain 규칙에 따른 상태 변경
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
