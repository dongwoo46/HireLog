package com.hirelog.api.report.presentation.controller

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.report.application.ReportReadService
import com.hirelog.api.report.application.ReportWriteService
import com.hirelog.api.report.domain.type.ReportStatus
import com.hirelog.api.report.domain.type.ReportTargetType
import com.hirelog.api.report.presentation.controller.dto.request.ReportAdminProcessReq
import com.hirelog.api.report.presentation.controller.dto.response.ReportRes
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
class ReportAdminController(
    private val readService: ReportReadService,
    private val writeService: ReportWriteService
) {

    @GetMapping
    fun getReports(
        @RequestParam status: ReportStatus?,
        @RequestParam targetType: ReportTargetType?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResult<ReportRes>> {
        val result = readService.findAll(
            status = status,
            targetType = targetType,
            page = page,
            size = size
        ).map { ReportRes.from(it) }
        return ResponseEntity.ok(result)
    }

    @PatchMapping("/{id}/process")
    fun processReport(
        @PathVariable id: Long,
        @AuthenticationPrincipal member: AuthenticatedMember,
        @RequestBody @Valid request: ReportAdminProcessReq
    ): ResponseEntity<Void> {
        writeService.processByAdmin(
            reportId = id,
            adminMemberId = member.memberId,
            processType = request.processType
        )
        return ResponseEntity.noContent().build()
    }
}
