package com.hirelog.api.relation.presentation.controller.dto


import com.hirelog.api.relation.domain.type.BrandPositionSource
import com.hirelog.api.relation.domain.type.BrandPositionStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

/**
 * BrandPosition displayName 변경 요청
 */
data class BrandPositionDisplayNameChangeReq(

    @field:Size(max = 200)
    @NotBlank
    val displayName: String
)

/**
 * BrandPosition 상태 변경 요청
 */
data class BrandPositionStatusChangeReq(

    @field:NotNull
    val status: BrandPositionStatus
)

/**
 * BrandPosition 명시적 생성 요청
 */
data class BrandPositionCreateReq(

    @field:NotNull
    @field:Positive
    val brandId: Long,

    @field:NotNull
    @field:Positive
    val positionId: Long,

    /**
     * 브랜드 내부 포지션명 (선택)
     */
    @field:Size(max = 200)
    @field:NotNull
    val displayName: String,

    @field:NotNull
    val source: BrandPositionSource
)

data class BrandPositionSearchReq(

    /**
     * 브랜드 ID (선택)
     * - 특정 브랜드 소속만 보고 싶을 때 사용
     */
    val brandId: Long? = null,

    /**
     * 브랜드 내부 포지션명 (부분 검색)
     */
    val displayName: String? = null,

    /**
     * BrandPosition 상태
     */
    val status: BrandPositionStatus? = null,

    /**
     * 생성 출처
     */
    val source: BrandPositionSource? = null,

    /**
     * 승인 여부
     */
    val approved: Boolean? = null
)
