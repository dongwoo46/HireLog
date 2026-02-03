package com.hirelog.api.company.presentation.controller

import com.hirelog.api.company.application.CompanyWriteService
import com.hirelog.api.company.application.port.CompanyQuery
import com.hirelog.api.company.application.view.CompanyNameView
import com.hirelog.api.company.application.view.CompanyView
import com.hirelog.api.company.presentation.controller.dto.CompanyCreateReq
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/company")
class CompanyController(
    private val companyWriteService: CompanyWriteService,
    private val companyQuery: CompanyQuery
) {

    /**
     * Company 생성 (getOrCreate)
     *
     * 정책:
     * - 동일 normalizedName 존재 시 기존 반환
     * - 없으면 신규 생성
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    fun create(
        @Valid @RequestBody request: CompanyCreateReq
    ): ResponseEntity<CompanyView> {

        val company = companyWriteService.getOrCreate(
            name = request.name,
            aliases = request.aliases,
            source = request.source,
            externalId = request.externalId
        )

        val view = companyQuery.findViewById(company.id)
            ?: throw IllegalStateException("Company 생성 직후 조회 실패: ${company.id}")

        return ResponseEntity.status(HttpStatus.CREATED).body(view)
    }

    /**
     * Company 단건 조회
     */
    @GetMapping("/{companyId}")
    fun getById(
        @PathVariable companyId: Long
    ): ResponseEntity<CompanyView> {

        val view = companyQuery.findViewById(companyId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(view)
    }

    /**
     * Company 전체 목록 조회 (관리자용)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    fun getAll(
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<CompanyView>> {

        val result = companyQuery.findAllViews(pageable)
        return ResponseEntity.ok(result)
    }

    /**
     * 활성 Company 목록 조회
     */
    @GetMapping("/active")
    fun getAllActive(
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<CompanyView>> {

        val result = companyQuery.findAllActiveViews(pageable)
        return ResponseEntity.ok(result)
    }

    /**
     * Company 이름 목록 조회 (자동완성용)
     */
    @GetMapping("/names")
    fun getAllNames(): ResponseEntity<List<CompanyNameView>> {

        val result = companyQuery.findAllNames()
        return ResponseEntity.ok(result)
    }

    /**
     * Company 검증 승인
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{companyId}/verify")
    fun verify(
        @PathVariable companyId: Long
    ): ResponseEntity<Void> {

        companyWriteService.verify(companyId)
        return ResponseEntity.ok().build()
    }

    /**
     * Company 비활성화
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{companyId}/deactivate")
    fun deactivate(
        @PathVariable companyId: Long
    ): ResponseEntity<Void> {

        companyWriteService.deactivate(companyId)
        return ResponseEntity.ok().build()
    }
}
