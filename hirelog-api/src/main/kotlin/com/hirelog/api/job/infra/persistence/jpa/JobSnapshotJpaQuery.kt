package com.hirelog.api.job.infrastructure.persistence.jpa

import com.hirelog.api.job.application.snapshot.query.JobSnapshotQuery
import com.hirelog.api.job.domain.JobSnapshot
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

/**
 * JobSnapshot 조회 JPA Adapter
 *
 * 책임:
 * - JobSnapshotQuery Port 구현
 * - JPA 기반 조회 수행
 *
 * 설계 원칙:
 * - Entity를 그대로 반환한다
 * - View/DTO 변환은 상위 계층 책임이다
 */
@Component
class JobSnapshotJpaQuery(
    private val repository: JobSnapshotJpaRepository
) : JobSnapshotQuery {

    /**
     * 콘텐츠 해시 기준 Snapshot 조회
     */
    override fun findByContentHash(contentHash: String): JobSnapshot? =
        repository.findByContentHash(contentHash)

    /**
     * ID 기준 Snapshot 조회
     */
    override fun findById(snapshotId: Long): JobSnapshot? =
        repository.findById(snapshotId).orElse(null)

    /**
     * 브랜드 + 포지션 기준 최신 Snapshot 조회
     *
     * 기준:
     * - createdAt 내림차순
     */
    override fun findLatestByBrandAndPosition(
        brandId: Long,
        positionId: Long
    ): JobSnapshot? =
        repository
            .findByBrandIdAndPositionIdOrderByCreatedAtDesc(
                brandId,
                positionId,
                PageRequest.of(0, 1)
            )
            .firstOrNull()

    /**
     * 브랜드 + 포지션 기준 전체 Snapshot 조회
     */
    override fun findAllByBrandAndPosition(
        brandId: Long,
        positionId: Long
    ): List<JobSnapshot> =
        repository.findAllByBrandIdAndPositionIdOrderByCreatedAtDesc(
            brandId,
            positionId
        )
}
