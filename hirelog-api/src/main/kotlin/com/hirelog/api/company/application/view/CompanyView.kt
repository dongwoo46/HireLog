package com.hirelog.api.company.application.view

import com.hirelog.api.common.domain.VerificationStatus
import com.hirelog.api.company.domain.CompanySource
import java.time.LocalDateTime

/**
 * CompanyView
 *
 * 책임:
 * - Company 관리/조회용 View
 * - 승인/활성화 판단에 필요한 정보만 포함
 */
data class CompanyView(
    val id: Long,
    val name: String,
    val source: CompanySource,
    val verificationStatus: VerificationStatus,
    val isActive: Boolean,
    val createdAt: LocalDateTime
)
