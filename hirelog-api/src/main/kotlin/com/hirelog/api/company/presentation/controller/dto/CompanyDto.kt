package com.hirelog.api.company.presentation.controller.dto

import com.hirelog.api.common.domain.VerificationStatus
import com.hirelog.api.company.domain.CompanySource
import jakarta.validation.constraints.NotBlank

/**
 * Company 생성 요청
 */
data class CompanyCreateReq(
    @field:NotBlank
    val name: String,
    val source: CompanySource = CompanySource.ADMIN,
    val externalId: String? = null
)

data class CompanySearchReq(

    val name: String? = null,

    val source: CompanySource? = null,

    val isActive: Boolean? = null,

    val verificationStatus: VerificationStatus? = null
)