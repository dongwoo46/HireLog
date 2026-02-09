package com.hirelog.api.relation.application.memberjobsummary.view

import com.hirelog.api.job.domain.type.HiringStage
import com.hirelog.api.relation.domain.type.HiringStageResult
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import java.time.LocalDateTime

/**
 * MemberJobSummary 상세 조회 View
 *
 * 용도:
 * - 저장된 JD 상세 화면
 * - 채용 단계 관리
 */
data class MemberJobSummaryDetailView(

    val memberJobSummaryId: Long,

    val jobSummaryId: Long,

    val brandName: String,

    val positionName: String,

    val brandPositionName: String,

    val positionCategoryName: String,

    val saveType: MemberJobSummarySaveType,

    val stages: List<HiringStageView>?,   // nullable

    val createdAt: LocalDateTime,

    val updatedAt: LocalDateTime
)

/**
 * 채용 단계 기록 View
 */
data class HiringStageView(

    val stage: HiringStage,

    val note: String,

    val result: HiringStageResult?,

    val recordedAt: LocalDateTime
)
