package com.hirelog.api.report.presentation.controller.dto.request

import com.hirelog.api.report.domain.type.ReportReason
import com.hirelog.api.report.domain.type.ReportTargetType
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class ReportWriteReq(
    @field:NotNull val targetType: ReportTargetType,
    @field:NotNull val targetId: Long,
    @field:NotNull val reason: ReportReason,
    @field:Size(max = 1000) val detail: String?
)
