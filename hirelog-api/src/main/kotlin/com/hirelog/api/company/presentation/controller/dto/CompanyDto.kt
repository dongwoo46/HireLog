package com.hirelog.api.company.presentation.controller.dto

import com.hirelog.api.company.domain.CompanyRelationType
import com.hirelog.api.company.domain.CompanySource
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/**
 * Company 생성 요청
 */
data class CompanyCreateReq(
    @field:NotBlank
    val name: String,
    val aliases: List<String> = emptyList(),
    val source: CompanySource = CompanySource.ADMIN,
    val externalId: String? = null
)

/**
 * CompanyRelation 생성 요청
 */
data class CompanyRelationCreateReq(
    @field:NotNull
    val parentCompanyId: Long,
    @field:NotNull
    val childCompanyId: Long,
    @field:NotNull
    val relationType: CompanyRelationType
)
