package com.hirelog.api.brand.application.view

import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.common.domain.VerificationStatus
import com.hirelog.api.relation.domain.type.BrandPositionStatus
import java.time.LocalDateTime

/**
 * Brand 상세 조회 View
 *
 * 포함 정보:
 * - Brand 기본 정보
 * - 소속 Company (nullable)
 * - 연관된 BrandPosition 목록
 */
data class BrandDetailView(

    val id: Long,
    val name: String,
    val normalizedName: String,

    /**
     * 소속 회사 (검증 전 / 미매핑 시 null)
     */
    val company: CompanySimpleView?,

    val verificationStatus: VerificationStatus,
    val source: BrandSource,
    val isActive: Boolean,

    val createdAt: LocalDateTime,

    /**
     * 연관 BrandPosition 목록
     *
     * - 매핑이 없으면 emptyList
     */
    val brandPositions: List<BrandPositionView>
)


/**
 * BrandPosition 조회용 View
 *
 * 목적:
 * - Brand 상세 화면에서 포지션 연결 정보 제공
 */
data class BrandPositionView(

    val id: Long,
    val positionId: Long,
    /**
     * 브랜드 기준 표시명
     */
    val displayName: String,

    val status: BrandPositionStatus
)

/**
 * Brand 상세 조회 시 포함되는 Company 요약 View
 *
 * 목적:
 * - Brand 소속 회사 표시
 * - 식별 및 링크용
 */
data class CompanySimpleView(

    val id: Long,
    val name: String,
    val isActive: Boolean
)
