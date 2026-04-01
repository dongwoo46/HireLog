package com.hirelog.api.report.application

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.report.application.port.ReportQuery
import com.hirelog.api.report.application.view.ReportView
import com.hirelog.api.report.domain.type.ReportStatus
import com.hirelog.api.report.domain.type.ReportTargetType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReportReadService(
    private val query: ReportQuery
) {

    @Transactional(readOnly = true)
    fun findAll(
        status: ReportStatus?,
        targetType: ReportTargetType?,
        page: Int,
        size: Int
    ): PagedResult<ReportView> {
        return query.findAll(
            status = status,
            targetType = targetType,
            page = page,
            size = size
        )
    }
}
