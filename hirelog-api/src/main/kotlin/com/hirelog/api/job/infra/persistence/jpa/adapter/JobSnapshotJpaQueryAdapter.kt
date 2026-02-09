package com.hirelog.api.job.infra.persistence.jpa.adapter

import com.hirelog.api.job.application.snapshot.port.JobSnapshotQuery
import com.hirelog.api.job.application.snapshot.view.JobSnapshotView
import com.hirelog.api.job.infra.persistence.jpa.mapper.toSnapshotView
import com.hirelog.api.job.infra.persistence.jpa.repository.JobSnapshotJpaRepository
import org.springframework.stereotype.Repository

/**
 * JobSnapshot JPA Query Adapter
 *
 * 책임:
 * - JobSnapshotQuery Port의 JPA 구현체
 *
 * 설계 원칙:
 * - 조회 결과는 반드시 Read Model(View)로 변환하여 반환
 * - Entity를 반환하는 메서드는 Command Adapter에 위치
 */
@Repository
class JobSnapshotJpaQueryAdapter(
    private val repository: JobSnapshotJpaRepository
) : JobSnapshotQuery {

    override fun getSnapshot(snapshotId: Long): JobSnapshotView? {
        return repository.findById(snapshotId)
            .map { it.toSnapshotView() }
            .orElse(null)
    }

    override fun getSnapshotByCanonicalHash(
        canonicalHash: String
    ): JobSnapshotView? {
        return repository.findByCanonicalHash(canonicalHash)
            ?.toSnapshotView()
    }

    override fun listSnapshotsForPosition(
        brandId: Long,
        positionId: Long
    ): List<JobSnapshotView> {
        return repository
            .findAllByBrandIdAndPositionIdOrderByCreatedAtDesc(
                brandId,
                positionId
            )
            .map { it.toSnapshotView() }
    }

    override fun getLatestSnapshotForPosition(
        brandId: Long,
        positionId: Long
    ): JobSnapshotView? {
        return repository
            .findFirstByBrandIdAndPositionIdOrderByCreatedAtDesc(
                brandId,
                positionId
            )
            ?.toSnapshotView()
    }
}
