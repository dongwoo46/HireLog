package com.hirelog.api.job.infra.persistence.jpa.adapter

import com.hirelog.api.job.application.snapshot.port.JobSnapshotCommand
import com.hirelog.api.job.domain.model.JobSnapshot
import com.hirelog.api.job.infra.persistence.jpa.repository.JobSnapshotJpaQueryDslRepository
import com.hirelog.api.job.infra.persistence.jpa.repository.JobSnapshotJpaRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * JobSnapshot JPA Command Adapter
 *
 * 책임:
 * - JobSnapshotCommand Port의 JPA 구현
 * - Entity 영속화 + Entity 로딩
 *
 * 주의:
 * - 트랜잭션 경계는 ApplicationService에서만 관리
 */
@Component
class JobSnapshotJpaCommandAdapter(
    private val repository: JobSnapshotJpaRepository,
    private val queryDslRepository: JobSnapshotJpaQueryDslRepository,
    private val entityManager: EntityManager
) : JobSnapshotCommand {

    override fun record(snapshot: JobSnapshot): Long {
        return repository.save(snapshot).id
    }

    override fun update(snapshot: JobSnapshot) {
        repository.save(snapshot)
    }

    override fun findById(id: Long): JobSnapshot? {
        return repository.findById(id).orElse(null)
    }

    override fun findAllBySourceUrl(url: String): List<JobSnapshot> {
        return repository.findAllBySourceUrl(url)
    }

    override fun findAllByDateRange(
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

    override fun findSimilarByCoreText(
        coreText: String,
        threshold: Double
    ): List<JobSnapshot> {
        val sql = """
            SELECT *
            FROM job_snapshot
            WHERE similarity(core_text, :coreText) >= :threshold
            ORDER BY similarity(core_text, :coreText) DESC
            LIMIT 10
        """.trimIndent()

        return entityManager
            .createNativeQuery(sql, JobSnapshot::class.java)
            .setParameter("coreText", coreText)
            .setParameter("threshold", threshold)
            .resultList
            .map { it as JobSnapshot }
    }
}
