package com.hirelog.api.brand.presentation.controller.dto

import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.common.domain.VerificationStatus
import java.time.LocalDateTime

/**
 * Brand 검색 조건
 *
 * 사용처:
 * - Brand 목록 조회
 */
data class BrandSearchReq(

    /**
     * 활성 여부
     */
    val isActive: Boolean? = null,

    /**
     * 검증 상태
     */
    val verificationStatus: VerificationStatus? = null,

    /**
     * 브랜드명 (부분 검색)
     */
    val name: String? = null,

    /**
     * 데이터 출처
     */
    val source: BrandSource? = null,

    /**
     * 생성일 시작 (inclusive)
     */
    val createdFrom: LocalDateTime? = null,

    /**
     * 생성일 종료 (exclusive)
     */
    val createdTo: LocalDateTime? = null
)
