package com.hirelog.api.job.infra.persistence.jpa.adapter

import com.hirelog.api.job.application.snapshot.port.JobSnapshotQuery
import com.hirelog.api.job.application.snapshot.view.JobSnapshotView
import com.hirelog.api.job.domain.JobSnapshot
import com.hirelog.api.job.infra.persistence.jpa.mapper.toSnapshotView
import com.hirelog.api.job.infra.persistence.jpa.repository.JobSnapshotJpaQueryDslRepository
import com.hirelog.api.job.infra.persistence.jpa.repository.JobSnapshotJpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * JobSnapshot JPA Query Adapter
 *
 * 책임:
 * - JobSnapshotQuery Port의 JPA 구현체
 *
 * 설계 원칙:
 * - JPA Entity는 Application 계층에 직접 노출하지 않는다
 * - 조회 결과는 반드시 Read Model(View)로 변환하여 반환한다
 * - 단, Command 흐름 준비를 위한 Entity 로딩은 제한적으로 허용한다
 */
@Repository
class JobSnapshotJpaQueryAdapter(
    private val repository: JobSnapshotJpaRepository,
    private val queryDslRepository: JobSnapshotJpaQueryDslRepository
) : JobSnapshotQuery {

    /**
     * Snapshot 단건 조회 (View 반환)
     */
    override fun getSnapshot(snapshotId: Long): JobSnapshotView? {
        return repository.findById(snapshotId)
            .map { it.toSnapshotView() }
            .orElse(null)
    }

    /**
     * canonicalHash 기준 Snapshot 조회 (View 반환)
     */
    override fun getSnapshotByContentHash(
        contentHash: String
    ): JobSnapshotView? {
        return repository.findByContentHash(contentHash)
            ?.toSnapshotView()
    }

    /**
     * 브랜드 + 포지션 기준 Snapshot 목록 조회 (View 반환)
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
            .map { it.toSnapshotView() }
    }

    /**
     * 브랜드 + 포지션 기준 최신 Snapshot 조회 (View 반환)
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
            ?.toSnapshotView()
    }

    /**
     * Snapshot Entity 로딩 (Command 전용)
     *
     * 주의:
     * - 상태 변경 유스케이스에서만 사용
     * - 조회/화면/분석 용도로 사용 ❌
     */
    override fun loadSnapshot(snapshotId: Long): JobSnapshot {
        return repository.findById(snapshotId)
            .orElseThrow { IllegalStateException("Snapshot not found: $snapshotId") }
    }

    /**
     * URL 기준 Snapshot Entity 조회 (Command 전용)
     */
    override fun loadSnapshotsByUrl(url: String): List<JobSnapshot> {
        return repository.findAllBySourceUrl(url)
    }

    /**
     * 기간(openedDate / closedDate) 기준 Snapshot Entity 조회 (Command 전용)
     *
     * 방어 로직:
     * - 날짜 정보가 모두 없는 경우 전체 스캔 방지
     */
    override fun loadSnapshotsByDateRange(
        openedDate: LocalDate?,
        closedDate: LocalDate?
    ): List<JobSnapshot> {
        if (openedDate == null && closedDate == null) {
            return emptyList()
        }

        return queryDslRepository.findAllOverlappingDateRange(
            openedDate = openedDate,
            closedDate = closedDate
        )
    }
}
