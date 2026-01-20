package com.hirelog.api.job.infra.persistence.jpa.mapper

import com.hirelog.api.job.application.snapshot.view.JobSnapshotView
import com.hirelog.api.job.domain.JobSnapshot

/**
 * JobSnapshot JPA Entity → Read Model(View) 변환 Mapper
 *
 * 책임:
 * - JPA Entity를 Application 계층의 Read Model로 변환
 *
 * 설계 원칙:
 * - Domain(Entity)은 View를 모른다
 * - Query Adapter에서만 사용한다
 * - 비즈니스 로직은 절대 포함하지 않는다
 */
fun JobSnapshot.toSnapshopView(): JobSnapshotView {
    return JobSnapshotView(
        id = this.id,
        brandId = this.brandId,
        companyId = this.companyId,
        positionId = this.positionId,
        sourceType = this.sourceType.name,
        sourceUrl = this.sourceUrl,
        rawText = this.rawText,
        contentHash = this.contentHash
    )
}
