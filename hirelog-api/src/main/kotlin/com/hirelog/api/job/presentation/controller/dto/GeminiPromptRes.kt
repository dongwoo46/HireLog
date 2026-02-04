package com.hirelog.api.job.presentation.controller.dto

/**
 * Gemini 프롬프트 조회 응답
 */
data class GeminiPromptRes(
    val systemInstruction: String,
    val userPrompt: String? = null
)
