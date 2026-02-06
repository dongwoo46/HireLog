package com.hirelog.api.company.presentation.controller

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.company.application.CompanyCandidateReadService
import com.hirelog.api.company.application.CompanyCandidateWriteService
import com.hirelog.api.company.application.view.CompanyCandidateDetailView
import com.hirelog.api.company.application.view.CompanyCandidateListView
import com.hirelog.api.company.presentation.controller.dto.CompanyCandidateSearchReq
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * CompanyCandidateController
 *
 * 책임:
 * - CompanyCandidate HTTP API 노출
 * - Read / Write Service 위임
 *
 * 정책:
 * - 관리자 전용 API
 */
@RestController
@RequestMapping("/api/company-candidates")
@PreAuthorize("hasRole('ADMIN')")
class CompanyCandidateController(
    private val readService: CompanyCandidateReadService,
    private val writeService: CompanyCandidateWriteService
) {

    /**
     * CompanyCandidate 목록 조회
     *
     * 기능:
     * - 조건 기반 검색
     * - Offset 페이징
     */
    @GetMapping
    fun search(
        @ModelAttribute condition: CompanyCandidateSearchReq,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<PagedResult<CompanyCandidateListView>> {

        return ResponseEntity.ok(
            readService.search(condition, pageable)
        )
    }

    /**
     * CompanyCandidate 상세 조회
     *
     * 의미:
     * - 후보 단건 + Brand / JD 정보
     */
    @GetMapping("/{candidateId}")
    fun getDetail(
        @PathVariable candidateId: Long
    ): ResponseEntity<CompanyCandidateDetailView> {

        val result = readService.getDetail(candidateId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(result)
    }

    /**
     * CompanyCandidate 승인
     */
    @PatchMapping("/{candidateId}/approve")
    fun approve(
        @PathVariable candidateId: Long
    ): ResponseEntity<Void> {

        writeService.approve(candidateId)
        return ResponseEntity.ok().build()
    }

    /**
     * CompanyCandidate 거절
     */
    @PatchMapping("/{candidateId}/reject")
    fun reject(
        @PathVariable candidateId: Long
    ): ResponseEntity<Void> {

        writeService.reject(candidateId)
        return ResponseEntity.ok().build()
    }
}
