package com.hirelog.api.report.application.port

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.report.application.view.ReportView
import com.hirelog.api.report.domain.type.ReportStatus
import com.hirelog.api.report.domain.type.ReportTargetType

interface ReportQuery {
    fun findAll(
        status: ReportStatus?,
        targetType: ReportTargetType?,
        page: Int,
        size: Int
    ): PagedResult<ReportView>
}
