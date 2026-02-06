package com.hirelog.api.position.presentation.controller.dto

import com.hirelog.api.position.domain.PositionStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * PositionCategory 생성 요청 DTO
 *
 * 정책:
 * - 관리자만 생성
 * - normalizedName은 서버에서 생성
 */
data class PositionCategoryCreateReq(

    /**
     * 카테고리명
     * 예: "IT / Software"
     */
    @field:NotBlank
    @field:Size(max = 200)
    val name: String,

    /**
     * 카테고리 설명 (선택)
     */
    @field:Size(max = 500)
    val description: String? = null
)

data class PositionCategorySearchReq(

    /**
     * 상태 필터
     * ACTIVE / INACTIVE
     */
    val status: PositionStatus? = null,

    /**
     * 카테고리명 부분 검색
     */
    val name: String? = null,

    /**
     * 페이지 번호 (0-based)
     */
    val page: Int = 0,

    /**
     * 페이지 크기
     */
    val size: Int = 20
)