package com.hirelog.api.brand.application.view

import com.hirelog.api.common.domain.VerificationStatus

data class BrandSummaryView(
    val id: Long,
    val name: String,
    val verificationStatus: VerificationStatus,
    val isActive: Boolean
)
