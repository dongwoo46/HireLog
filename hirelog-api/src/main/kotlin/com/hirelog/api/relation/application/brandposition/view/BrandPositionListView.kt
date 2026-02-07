package com.hirelog.api.relation.application.brandposition.view

import com.hirelog.api.relation.domain.type.BrandPositionSource
import com.hirelog.api.relation.domain.type.BrandPositionStatus
import java.time.LocalDateTime

/**
 * BrandPosition 목록 조회 View
 */
data class BrandPositionListView(

    val id: Long,
    val brandId: Long,
    val positionId: Long,

    /**
     * 브랜드 내부 포지션명
     */
    val displayName: String?,

    val status: BrandPositionStatus,
    val source: BrandPositionSource,

    val approvedAt: LocalDateTime?,
    val approvedBy: Long?
)
