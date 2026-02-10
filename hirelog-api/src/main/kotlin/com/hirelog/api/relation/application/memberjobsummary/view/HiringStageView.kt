package com.hirelog.api.relation.application.memberjobsummary.view

import com.hirelog.api.job.domain.type.HiringStage
import com.hirelog.api.relation.domain.type.HiringStageResult
import java.time.LocalDateTime

/**
 * 채용 단계 기록 View
 */
data class HiringStageView(

    val stage: HiringStage,

    val note: String,

    val result: HiringStageResult?,

    val recordedAt: LocalDateTime
)
