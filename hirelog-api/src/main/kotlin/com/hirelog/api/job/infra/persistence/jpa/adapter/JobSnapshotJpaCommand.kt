package com.hirelog.api.job.infra.persistence.jpa.adapter

import com.hirelog.api.job.application.snapshot.command.JobSnapshotCommand
import com.hirelog.api.job.domain.JobSnapshot
import com.hirelog.api.job.domain.JobSourceType
import com.hirelog.api.job.infra.persistence.jpa.repository.JobSnapshotJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * JobSnapshot JPA Command Adapter
 *
 * 책임:
 * - JobSnapshotCommand Port의 JPA 구현
 * - 트랜잭션 내부에서 Entity 저장
 */
@Component
class JobSnapshotJpaCommand(
    private val repository: JobSnapshotJpaRepository
) : JobSnapshotCommand {

    /**
     * JobSnapshot 생성 및 저장
     */
    @Transactional
    override fun create(
        brandId: Long,
        companyId: Long?,
        positionId: Long,
        sourceType: JobSourceType,
        sourceUrl: String?,
        rawText: String,
        contentHash: String
    ): JobSnapshot {

        val snapshot = JobSnapshot.create(
            brandId = brandId,
            companyId = companyId,
            positionId = positionId,
            sourceType = sourceType,
            sourceUrl = sourceUrl,
            rawText = rawText,
            contentHash = contentHash
        )

        return repository.save(snapshot)
    }
}
