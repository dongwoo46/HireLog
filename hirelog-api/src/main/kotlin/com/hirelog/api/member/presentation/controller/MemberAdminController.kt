package com.hirelog.api.member.presentation.controller

import com.hirelog.api.member.application.MemberWriteService
import com.hirelog.api.member.application.port.MemberQuery
import com.hirelog.api.member.application.view.MemberDetailView
import com.hirelog.api.member.application.view.MemberSummaryView
import com.hirelog.api.userrequest.application.port.PagedResult
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * MemberAdminController
 *
 * 관리자 전용 회원 관리 API
 */
@RestController
@RequestMapping("/api/admin/member")
@PreAuthorize("hasRole('ADMIN')")
class MemberAdminController(
    private val memberWriteService: MemberWriteService,
    private val memberQuery: MemberQuery
) {

    /**
     * 회원 목록 조회
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    fun getAll(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResult<MemberSummaryView>> {

        val result = memberQuery.findAllPaged(page, size)
        return ResponseEntity.ok(result)
    }

    /**
     * 회원 상세 조회
     */
    @GetMapping("/{memberId}")
    fun getById(
        @PathVariable memberId: Long
    ): ResponseEntity<MemberDetailView> {

        val view = memberQuery.findDetailById(memberId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(view)
    }

    /**
     * 회원 정지
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{memberId}/suspend")
    fun suspend(
        @PathVariable memberId: Long
    ): ResponseEntity<Void> {

        memberWriteService.suspend(memberId)
        return ResponseEntity.ok().build()
    }

    /**
     * 회원 활성화
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{memberId}/activate")
    fun activate(
        @PathVariable memberId: Long
    ): ResponseEntity<Void> {

        memberWriteService.activate(memberId)
        return ResponseEntity.ok().build()
    }

    /**
     * 회원 삭제 (논리 삭제)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{memberId}")
    fun delete(
        @PathVariable memberId: Long
    ): ResponseEntity<Void> {

        memberWriteService.delete(memberId)
        return ResponseEntity.noContent().build()
    }
}
