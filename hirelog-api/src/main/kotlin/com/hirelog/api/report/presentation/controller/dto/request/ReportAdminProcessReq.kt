package com.hirelog.api.report.presentation.controller.dto.request

import com.hirelog.api.report.application.ReportWriteService
import jakarta.validation.constraints.NotNull

data class ReportAdminProcessReq(
    @field:NotNull
    val processType: ReportWriteService.AdminProcessType
)
