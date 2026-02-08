package com.hirelog.api.job.application.summary.view

import com.hirelog.api.job.domain.CareerType

/**
 * JobSummary 조회 결과 View
 *
 * 특징:
 * - 읽기 전용
 * - JPA / OpenSearch 공통 표현
 * - Entity와 분리
 */
data class JobSummaryView(

    val summaryId: Long,
    val snapshotId: Long,

    val brandId: Long,
    val brandName: String,

    val positionId: Long,
    val positionName: String,

    val brandPositionId: Long?,
    val positionCategoryId: Long,
    val positionCategoryName: String,

    val careerType: CareerType,
    val careerYears: Int?,

    val summary: String,
    val responsibilities: String,
    val requiredQualifications: String,
    val preferredQualifications: String?,

    val techStack: String?
)
