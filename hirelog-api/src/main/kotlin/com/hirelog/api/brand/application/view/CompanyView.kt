package com.hirelog.api.brand.application.view

import com.hirelog.api.common.domain.VerificationStatus

/**
 * CompanyView
 *
 * 책임:
 * - Brand 상세 조회 시 포함되는 회사 정보 View
 * - QueryDSL Projection 전용 Read Model
 *
 * 원칙:
 * - Domain Entity 의존 ❌
 * - 비즈니스 로직 ❌
 */
data class CompanyView(
    val id: Long,
    val name: String,
    val normalizedName: String,
    val isActive: Boolean
)
