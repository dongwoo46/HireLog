package com.hirelog.api.job.infra.persistence.jpa.projection

import com.hirelog.api.job.domain.type.CareerType

/**
 * JobSummary 조회 전용 Projection
 *
 * 특징:
 * - JPA Entity 아님
 * - 조회 결과 표현 전용
 * - 영속성 컨텍스트 / Lazy 로딩 없음
 */
interface JobSummaryProjection {

    val summaryId: Long
    val snapshotId: Long

    val brandId: Long
    val brandName: String

    val positionId: Long
    val positionName: String

    val brandPositionId: Long?
    val positionCategoryId: Long
    val positionCategoryName: String

    val careerType: CareerType
    val careerYears: Int?

    val summary: String
    val responsibilities: String
    val requiredQualifications: String
    val preferredQualifications: String?

    val techStack: String?
}
