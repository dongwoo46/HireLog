package com.hirelog.api.brand.application.view

import com.hirelog.api.brand.domain.Brand

import com.hirelog.api.common.domain.VerificationStatus
import com.hirelog.api.brand.domain.BrandSource
import java.time.LocalDateTime

data class BrandDetailView(
    val id: Long,
    val name: String,
    val normalizedName: String,
    val verificationStatus: VerificationStatus,
    val source: BrandSource,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val company: CompanyView? // companyId 있을 때만
)

