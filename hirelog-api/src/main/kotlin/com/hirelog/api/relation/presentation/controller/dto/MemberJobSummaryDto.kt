package com.hirelog.api.relation.presentation.controller.dto

import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import jakarta.validation.constraints.NotNull

/**
 * JD 저장 요청
 */
data class SaveJobSummaryReq(
    @field:NotNull
    val jobSummaryId: Long,
    val saveType: MemberJobSummarySaveType = MemberJobSummarySaveType.SAVED,
    val memo: String? = null
)

/**
 * 저장 유형 변경 요청
 */
data class ChangeSaveTypeReq(
    @field:NotNull
    val jobSummaryId: Long,
    @field:NotNull
    val saveType: MemberJobSummarySaveType
)

/**
 * 메모 수정 요청
 */
data class UpdateMemoReq(
    @field:NotNull
    val jobSummaryId: Long,
    val memo: String?
)

/**
 * JD 저장 취소 요청
 */
data class UnsaveJobSummaryReq(
    @field:NotNull
    val jobSummaryId: Long
)
