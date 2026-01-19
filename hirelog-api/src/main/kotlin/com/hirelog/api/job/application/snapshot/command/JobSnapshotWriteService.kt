package com.hirelog.api.job.application.snapshot.command

import com.hirelog.api.job.domain.JobSnapshot
import com.hirelog.api.job.domain.JobSourceType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * JobSnapshot Write Service
 *
 * 책임:
 * - JobSnapshot 생성
 * - 트랜잭션 경계 정의
 *
 * 주의:
 * - 중복 정책 ❌
 * - 조회 판단 ❌
 */
@Service
class JobSnapshotWriteService(
    private val snapshotCommand: JobSnapshotCommand
) {

    /**
     * JobSnapshot 생성
     *
     * 책임:
     * - 트랜잭션 관리
     * - Snapshot 영속화
     *
     * 주의:
     * - sourceType / sourceUrl 정책 ❌
     * - 중복 판단 ❌
     */
    @Transactional
    fun create(
        brandId: Long,
        positionId: Long,
        sourceType: JobSourceType,
        sourceUrl: String?,
        rawText: String,
        contentHash: String
    ): JobSnapshot =
        snapshotCommand.create(
            brandId = brandId,
            companyId = null,
            positionId = positionId,
            sourceType = sourceType,
            sourceUrl = sourceUrl,
            rawText = rawText,
            contentHash = contentHash
        )
}
