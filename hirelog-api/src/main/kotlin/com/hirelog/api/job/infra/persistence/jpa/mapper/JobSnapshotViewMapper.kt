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
 * - null 값은 그대로 전달한다
 * - 비즈니스 판단 로직은 절대 포함하지 않는다
 */
fun JobSnapshot.toSnapshotView(): JobSnapshotView {
    return JobSnapshotView(
        id = this.id,
        brandId = this.brandId,          // 분석 전이면 null 그대로
        positionId = this.positionId,    // 분석 전이면 null 그대로
        sourceType = this.sourceType,
        sourceUrl = this.sourceUrl,      // OCR 등에서는 null 가능
        rawText = this.rawText,
        contentHash = this.contentHash
    )
}
