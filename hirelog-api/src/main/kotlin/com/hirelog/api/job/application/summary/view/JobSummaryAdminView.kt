package com.hirelog.api.job.application.summary.view

import com.hirelog.api.job.domain.type.CareerType
import java.time.LocalDateTime

/**
 * JobSummary Admin 목록 조회 View
 *
 * 용도:
 * - 관리자 페이지 목록 전용
 * - 활성화/비활성화 모두 포함
 */
data class JobSummaryAdminView(

    val summaryId: Long = 0L,

    val brandId: Long = 0L,
    val brandName: String = "",

    val positionId: Long = 0L,
    val positionName: String = "",
    val positionCategoryName: String = "",

    val careerType: CareerType = CareerType.UNKNOWN,
    val careerYears: String? = null,

    val isActive: Boolean = true,
    val sourceUrl: String? = null,

    val createdAt: LocalDateTime = LocalDateTime.now()
)