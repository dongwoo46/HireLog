package com.hirelog.api.brand.application.view

import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.common.domain.VerificationStatus

/**
 * Brand 목록 조회용 View
 *
 * 목적:
 * - 관리자 리스트 화면
 * - 가볍고 빠른 조회
 */
data class BrandListView(

    val id: Long,
    val name: String,
    val verificationStatus: VerificationStatus,
    val source: BrandSource,
    val isActive: Boolean
)
