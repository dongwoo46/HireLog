package com.hirelog.api.relation.application.memberjobsummary.view

import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import java.time.LocalDateTime

/**
 * MemberJobSummary 목록 조회 View
 */
data class MemberJobSummaryListView(

    val memberJobSummaryId: Long,

    val jobSummaryId: Long,

    val brandName: String,

    val positionName: String,

    val brandPositionName: String,

    val positionCategoryName: String,

    val saveType: MemberJobSummarySaveType,

    val createdAt: LocalDateTime
)
