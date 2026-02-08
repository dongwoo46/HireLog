package com.hirelog.api.job.infra.persistence.jpa.mapper.summary

import com.hirelog.api.job.application.summary.view.JobSummaryView
import com.hirelog.api.job.infra.persistence.jpa.projection.JobSummaryProjection

/**
 * JobSummaryProjection → JobSummaryView Mapper
 *
 * 책임:
 * - 조회 결과를 Application Read Model로 변환
 * - 필드 1:1 매핑만 수행
 */
fun JobSummaryProjection.toSummaryView(): JobSummaryView {
    return JobSummaryView(
        summaryId = summaryId,
        snapshotId = snapshotId,

        brandId = brandId,
        brandName = brandName,

        positionId = positionId,
        positionName = positionName,

        brandPositionId = brandPositionId,
        positionCategoryId = positionCategoryId,
        positionCategoryName = positionCategoryName,

        careerType = careerType,
        careerYears = careerYears,

        summary = summary,
        responsibilities = responsibilities,
        requiredQualifications = requiredQualifications,
        preferredQualifications = preferredQualifications,

        techStack = techStack
    )
}
