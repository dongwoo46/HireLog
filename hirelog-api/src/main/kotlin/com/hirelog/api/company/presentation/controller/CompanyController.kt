package com.hirelog.api.company.presentation.controller

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.common.config.security.CurrentUser
import com.hirelog.api.company.application.CompanyReadService
import com.hirelog.api.company.application.CompanyWriteService
import com.hirelog.api.company.application.view.CompanyDetailView
import com.hirelog.api.company.application.view.CompanyView
import com.hirelog.api.company.presentation.controller.dto.CompanyCreateReq
import com.hirelog.api.company.presentation.controller.dto.CompanyNameChangeReq
import com.hirelog.api.company.presentation.controller.dto.CompanySearchReq
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/companies")
@PreAuthorize("hasRole('ADMIN')")
class CompanyController(
    private val companyWriteService: CompanyWriteService,
    private val companyReadService: CompanyReadService
) {

    /**
     * Company 생성
     */
    @PostMapping
    fun create(
        @Valid @RequestBody request: CompanyCreateReq,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<Void> {

        companyWriteService.create(
            name = request.name,
            source = request.source,
            externalId = request.externalId,
            member = member
        )

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    /**
     * 회사명 변경
     */
    @PatchMapping("/{companyId}/name")
    fun changeName(
        @PathVariable companyId: Long,
        @Valid @RequestBody request: CompanyNameChangeReq
    ): ResponseEntity<Void> {

        companyWriteService.changeName(
            companyId = companyId,
            newName = request.name
        )

        return ResponseEntity.noContent().build()
    }


    /**
     * Company 목록 조회 (검색 + 페이징)
     */
    @GetMapping
    fun search(
        @ModelAttribute condition: CompanySearchReq,
        @PageableDefault(size = 20) pageable: Pageable,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<PagedResult<CompanyView>> {

        val result = companyReadService.search(condition, pageable)
        return ResponseEntity.ok(result)
    }

    /**
     * Company 상세 조회
     */
    @GetMapping("/{companyId}")
    fun getDetail(
        @PathVariable companyId: Long,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<CompanyDetailView> {

        val view = companyReadService.findDetail(companyId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(view)
    }

    /**
     * Company 활성화
     */
    @PatchMapping("/{companyId}/activate")
    fun activate(
        @PathVariable companyId: Long,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<Void> {

        companyWriteService.activate(companyId, member)
        return ResponseEntity.ok().build()
    }

    /**
     * Company 비활성화
     */
    @PatchMapping("/{companyId}/deactivate")
    fun deactivate(
        @PathVariable companyId: Long,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<Void> {

        companyWriteService.deactivate(companyId, member)
        return ResponseEntity.ok().build()
    }
}
