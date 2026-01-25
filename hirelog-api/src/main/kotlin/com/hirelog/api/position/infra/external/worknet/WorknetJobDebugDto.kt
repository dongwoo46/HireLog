package com.hirelog.api.position.presentation.debug.dto

/**
 * Worknet 직업 탐색용 DTO
 *
 * 목적:
 * - 사람이 읽기 쉬운 형태
 * - 패턴 분석 / 로그 확인
 *
 * 주의:
 * - 도메인 아님
 * - 저장용 아님
 */
data class WorknetJobDebugDto(
    val jobCode: String,
    val jobName: String,
    val categoryCode: String?,
    val categoryName: String?
)
