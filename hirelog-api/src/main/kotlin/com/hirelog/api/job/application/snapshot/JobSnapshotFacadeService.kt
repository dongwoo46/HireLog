package com.hirelog.api.job.application.snapshot.facade

import com.hirelog.api.common.utils.Hasher
import com.hirelog.api.job.application.snapshot.command.JobSnapshotWriteService
import com.hirelog.api.job.application.snapshot.query.JobSnapshotQuery
import com.hirelog.api.job.domain.JobSnapshot
import com.hirelog.api.job.domain.JobSourceType
import org.springframework.stereotype.Service

/**
 * JobSnapshot Facade Service
 *
 * 책임:
 * - Snapshot 관련 정책 결정
 * - 중복 판단
 * - 조회/생성 흐름 오케스트레이션
 *
 * 설계 원칙:
 * - 트랜잭션 ❌
 * - 저장은 WriteService에 위임
 */
@Service
class JobSnapshotFacadeService(
    private val snapshotQuery: JobSnapshotQuery,
    private val snapshotWriteService: JobSnapshotWriteService
) {

    /**
     * Snapshot 생성 (중복 방지)
     *
     * 정책:
     * - contentHash 기준 중복 허용 ❌
     * - sourceType / sourceUrl은 호출자가 결정
     */
    fun createIfNotExists(
        brandId: Long,
        positionId: Long,
        sourceType: JobSourceType,
        sourceUrl: String?,
        rawText: String
    ): JobSnapshot {

        val contentHash = Hasher.hash(rawText)

        snapshotQuery.findByContentHash(contentHash)
            ?.let { return it }

        return snapshotWriteService.create(
            brandId = brandId,
            positionId = positionId,
            sourceType = sourceType,
            sourceUrl = sourceUrl,
            rawText = rawText,
            contentHash = contentHash
        )
    }

}
