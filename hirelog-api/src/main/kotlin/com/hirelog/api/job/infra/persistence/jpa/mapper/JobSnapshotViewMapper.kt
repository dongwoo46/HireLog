package com.hirelog.api.job.infra.persistence.jpa.mapper

import com.hirelog.api.job.application.snapshot.view.JobSnapshotView
import com.hirelog.api.job.domain.model.JobSnapshot

/**
 * JobSnapshot JPA Entity → Read Model(View) 변환 Mapper
 *
 * 책임:
 * - JPA Entity를 Application 계층의 Read Model로 변환
 *
 * 설계 원칙:
 * - Domain(Entity)은 View를 모른다
 * - Query Adapter에서만 사용한다
 * - 상태 변경 로직 ❌
 * - 판단 로직 ❌
 */
fun JobSnapshot.toSnapshotView(): JobSnapshotView {
    return JobSnapshotView(
        id = this.id,

        brandId = this.brandId,
        positionId = this.positionId,

        sourceType = this.sourceType,
        sourceUrl = this.sourceUrl,

        canonicalSections = this.canonicalSections,

        recruitmentPeriodType = this.recruitmentPeriodType,
        openedDate = this.openedDate,
        closedDate = this.closedDate,

//        contentHash = this.canonicalHash,
    )
}
