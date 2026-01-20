package com.hirelog.api.job.infra.persistence.jpa.adapter

import com.hirelog.api.job.application.snapshot.port.JobSnapshotQuery
import com.hirelog.api.job.application.snapshot.view.JobSnapshotView
import com.hirelog.api.job.infra.persistence.jpa.mapper.toSnapshopView
import com.hirelog.api.job.infra.persistence.jpa.repository.JobSnapshotJpaRepository
import org.springframework.stereotype.Repository

/**
 * JobSnapshot JPA Query Adapter
 *
 * 책임:
 * - JobSnapshotQuery Port의 JPA 구현
 * - JPA Entity를 Read Model(View)로 변환하여 반환
 *
 * 설계 원칙:
 * - Application 계층에 Entity를 노출하지 않는다
 * - Query 결과는 항상 View(Read Model)로 반환한다
 * - 저장소(JPA) 세부 구현은 외부로 드러나지 않는다
 */
@Repository
class JobSnapshotJpaQueryAdapter(
    private val repository: JobSnapshotJpaRepository
) : JobSnapshotQuery {

    /**
     * Snapshot 단건 조회
     *
     * 사용 목적:
     * - 특정 Snapshot 상세 조회
     */
    override fun getSnapshot(
        snapshotId: Long
    ): JobSnapshotView? {
        return repository.findById(snapshotId)
            .map { it.toSnapshopView() }
            .orElse(null)
    }

    /**
     * canonicalHash 기준 Snapshot 조회
     *
     * 사용 목적:
     * - Snapshot 중복 판정
     */
    override fun getSnapshotByCanonicalHash(
        canonicalHash: String
    ): JobSnapshotView? {
        return repository.findByContentHash(canonicalHash)
            ?.toSnapshopView()
    }

    /**
     * 브랜드 + 포지션 기준 Snapshot 목록 조회
     *
     * 조회 정책:
     * - createdAt DESC (최신 순)
     */
    override fun listSnapshotsForPosition(
        brandId: Long,
        positionId: Long
    ): List<JobSnapshotView> {
        return repository
            .findAllByBrandIdAndPositionIdOrderByCreatedAtDesc(
                brandId,
                positionId
            )
            .map { it.toSnapshopView() }
    }

    /**
     * 브랜드 + 포지션 기준 최신 Snapshot 조회
     *
     * 정의:
     * - 가장 최근에 생성된 Snapshot 1건
     */
    override fun getLatestSnapshotForPosition(
        brandId: Long,
        positionId: Long
    ): JobSnapshotView? {
        return repository
            .findFirstByBrandIdAndPositionIdOrderByCreatedAtDesc(
                brandId,
                positionId
            )
            ?.toSnapshopView()
    }
}
