package com.hirelog.api.report.presentation.controller

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.report.application.ReportReadService
import com.hirelog.api.report.application.ReportWriteService
import com.hirelog.api.report.domain.type.ReportStatus
import com.hirelog.api.report.domain.type.ReportTargetType
import com.hirelog.api.report.presentation.controller.dto.request.ReportWriteReq
import com.hirelog.api.report.presentation.controller.dto.response.ReportRes
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val writeService: ReportWriteService,
    private val readService: ReportReadService
) {

    /**
     * 신고 접수
     * POST /api/reports
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    fun report(
        @AuthenticationPrincipal member: AuthenticatedMember,
        @RequestBody @Valid request: ReportWriteReq
    ): ResponseEntity<Map<String, Long>> {
        val report = writeService.report(
            reporterId = member.memberId,
            targetType = request.targetType,
            targetId = request.targetId,
            reason = request.reason,
            detail = request.detail
        )
        return ResponseEntity.status(201).body(mapOf("id" to report.id))
    }

    /**
     * 신고 목록 조회 (관리자)
     * GET /api/reports?status=PENDING&targetType=JOB_SUMMARY&page=0&size=20
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
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

    /**
     * 신고 검토 (관리자) PENDING → REVIEWED
     * PATCH /api/reports/{id}/review
     */
    @PatchMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    fun review(
        @PathVariable id: Long,
        @AuthenticationPrincipal member: AuthenticatedMember
    ): ResponseEntity<Void> {
        writeService.review(reportId = id, adminMemberId = member.memberId)
        return ResponseEntity.noContent().build()
    }

    /**
     * 신고 처리 (관리자) REVIEWED → RESOLVED
     * PATCH /api/reports/{id}/resolve
     */
    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    fun resolve(
        @PathVariable id: Long,
        @AuthenticationPrincipal member: AuthenticatedMember
    ): ResponseEntity<Void> {
        writeService.resolve(reportId = id, adminMemberId = member.memberId)
        return ResponseEntity.noContent().build()
    }

    /**
     * 신고 처리 + 대상 삭제 (관리자)
     * PATCH /api/reports/{id}/resolve-delete
     */
    @PatchMapping("/{id}/resolve-delete")
    @PreAuthorize("hasRole('ADMIN')")
    fun resolveAndDeleteTarget(
        @PathVariable id: Long,
        @AuthenticationPrincipal member: AuthenticatedMember
    ): ResponseEntity<Void> {
        writeService.resolveAndDeleteTarget(reportId = id, adminMemberId = member.memberId)
        return ResponseEntity.noContent().build()
    }

    /**
     * 신고 반려 (관리자) REVIEWED → REJECTED
     * PATCH /api/reports/{id}/reject
     */
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    fun reject(
        @PathVariable id: Long,
        @AuthenticationPrincipal member: AuthenticatedMember
    ): ResponseEntity<Void> {
        writeService.reject(reportId = id, adminMemberId = member.memberId)
        return ResponseEntity.noContent().build()
    }
}
