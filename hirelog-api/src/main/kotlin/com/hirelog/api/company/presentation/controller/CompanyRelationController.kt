package com.hirelog.api.company.presentation.controller

import com.hirelog.api.company.application.CompanyRelationWriteService
import com.hirelog.api.company.application.port.CompanyRelationQuery
import com.hirelog.api.company.application.view.CompanyRelationView
import com.hirelog.api.company.presentation.controller.dto.CompanyRelationCreateReq
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/company-relation")
class CompanyRelationController(
    private val companyRelationWriteService: CompanyRelationWriteService,
    private val companyRelationQuery: CompanyRelationQuery
) {

    /**
     * 회사 관계 생성
     *
     * 정책:
     * - (parent, child) 조합은 유일해야 함
     * - 중복 생성 시 409 Conflict
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    fun create(
        @Valid @RequestBody request: CompanyRelationCreateReq
    ): ResponseEntity<CompanyRelationView> {

        val relation = companyRelationWriteService.create(
            parentCompanyId = request.parentCompanyId,
            childCompanyId = request.childCompanyId,
            relationType = request.relationType
        )

        val view = companyRelationQuery.findView(
            parentCompanyId = relation.parentCompanyId,
            childCompanyId = relation.childCompanyId
        ) ?: throw IllegalStateException("CompanyRelation 생성 직후 조회 실패")

        return ResponseEntity.status(HttpStatus.CREATED).body(view)
    }

    /**
     * 회사 관계 삭제
     *
     * 정책:
     * - Idempotent delete
     * - 존재하지 않아도 204 반환
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    fun delete(
        @RequestParam parentCompanyId: Long,
        @RequestParam childCompanyId: Long
    ): ResponseEntity<Void> {

        companyRelationWriteService.delete(
            parentCompanyId = parentCompanyId,
            childCompanyId = childCompanyId
        )

        return ResponseEntity.noContent().build()
    }

    /**
     * 특정 회사의 자회사/계열사 목록 조회 (Parent 기준)
     */
    @GetMapping("/children")
    fun getChildren(
        @RequestParam parentCompanyId: Long,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<CompanyRelationView>> {

        val result = companyRelationQuery.findAllByParentCompanyId(
            parentCompanyId = parentCompanyId,
            pageable = pageable
        )

        return ResponseEntity.ok(result)
    }

    /**
     * 특정 회사의 모회사 목록 조회 (Child 기준)
     */
    @GetMapping("/parents")
    fun getParents(
        @RequestParam childCompanyId: Long,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<CompanyRelationView>> {

        val result = companyRelationQuery.findAllByChildCompanyId(
            childCompanyId = childCompanyId,
            pageable = pageable
        )

        return ResponseEntity.ok(result)
    }

    /**
     * 특정 관계 존재 여부 확인
     */
    @GetMapping("/exists")
    fun exists(
        @RequestParam parentCompanyId: Long,
        @RequestParam childCompanyId: Long
    ): ResponseEntity<Map<String, Boolean>> {

        val exists = companyRelationQuery.findView(
            parentCompanyId = parentCompanyId,
            childCompanyId = childCompanyId
        ) != null

        return ResponseEntity.ok(mapOf("exists" to exists))
    }
}
