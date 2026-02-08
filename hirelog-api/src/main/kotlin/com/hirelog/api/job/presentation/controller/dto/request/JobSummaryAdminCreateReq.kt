package com.hirelog.api.job.presentation.controller.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Admin 전용 JobSummary 직접 생성 요청
 *
 * Python 전처리 파이프라인 없이 직접 Gemini 호출
 */
data class JobSummaryAdminCreateReq(

    @field:NotBlank(message = "브랜드명은 필수입니다")
    @field:Size(max = 200, message = "브랜드명은 200자 이내여야 합니다")
    val brandName: String,

    @field:NotBlank(message = "포지션명은 필수입니다")
    @field:Size(max = 200, message = "포지션명은 200자 이내여야 합니다")
    val positionName: String,

    @field:NotBlank(message = "JD 원문은 필수입니다")
    @field:Size(max = 50000, message = "JD 원문은 50000자 이내여야 합니다")
    val jdText: String,

    @field:Size(max = 2000, message = "URL은 2000자 이내여야 합니다")
    val sourceUrl: String? = null
)
