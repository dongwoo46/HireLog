package com.hirelog.api.position.presentation.controller.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * Position 생성 요청 DTO
 *
 * 정책:
 * - 관리자만 생성
 * - normalizedName은 서버에서 생성
 */
data class PositionCreateReq(

    /**
     * 포지션명 (사람이 인식하는 이름)
     */
    @field:NotBlank
    @field:Size(max = 200)
    val name: String,

    /**
     * 포지션 설명 (선택)
     */
    @field:Size(max = 500)
    val description: String? = null,

    /**
     * 포지션 카테고리 ID
     */
    @field:NotNull
    val categoryId: Long
)


/**
 * Position 검색 조건 (관리자용)
 *
 * 특징:
 * - 모든 필드는 optional
 * - 조합 검색 가능
 */
data class PositionSearchReq(

    /**
     * 포지션 상태 필터
     */
    val status: String? = null,

    /**
     * 포지션 카테고리 ID
     */
    val categoryId: Long? = null,

    /**
     * 포지션명 검색어
     */
    val name: String? = null
)
