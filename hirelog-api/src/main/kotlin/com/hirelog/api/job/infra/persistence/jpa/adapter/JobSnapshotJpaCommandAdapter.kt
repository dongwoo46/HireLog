package com.hirelog.api.job.infra.persistence.jpa.adapter

import com.hirelog.api.job.application.snapshot.port.JobSnapshotCommand
import com.hirelog.api.job.domain.JobSnapshot
import com.hirelog.api.job.infra.persistence.jpa.repository.JobSnapshotJpaRepository
import org.springframework.stereotype.Component

/**
 * JobSnapshot JPA Command Adapter
 *
 * 책임:
 * - JobSnapshotCommand Port의 JPA 구현
 * - Entity 영속화 수행
 *
 * 주의:
 * - 트랜잭션 경계는 ApplicationService에서만 관리
 */
@Component
class JobSnapshotJpaCommandAdapter(
    private val repository: JobSnapshotJpaRepository
) : JobSnapshotCommand {

    /**
     * JobSnapshot 저장
     *
     * @return 생성된 Snapshot ID
     */
    override fun record(snapshot: JobSnapshot): Long {
        return repository.save(snapshot).id
    }

    /**
     * JobSnapshot 상태 변경 반영
     *
     * 전제:
     * - snapshot은 이미 식별자(id)를 가진 영속 대상
     * - brandId / positionId 설정은 Domain에서 이미 검증됨
     */
    override fun update(snapshot: JobSnapshot) {
        repository.save(snapshot)
    }
}
