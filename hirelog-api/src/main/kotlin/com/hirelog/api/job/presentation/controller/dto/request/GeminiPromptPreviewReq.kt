package com.hirelog.api.job.presentation.controller.dto.request

import jakarta.validation.constraints.NotBlank

/**
 * Gemini 프롬프트 미리보기 요청
 */
data class GeminiPromptPreviewReq(

    @field:NotBlank(message = "브랜드명은 필수입니다")
    val brandName: String,

    @field:NotBlank(message = "포지션명은 필수입니다")
    val positionName: String,

    @field:NotBlank(message = "JD 텍스트는 필수입니다")
    val jdText: String,

    val positionCandidates: List<String> = emptyList(),

    val existCompanies: List<String> = emptyList()
)
