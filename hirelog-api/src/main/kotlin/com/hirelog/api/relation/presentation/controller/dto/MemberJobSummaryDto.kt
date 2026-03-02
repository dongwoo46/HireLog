package com.hirelog.api.relation.application.view

import com.hirelog.api.job.domain.type.HiringStage
import com.hirelog.api.relation.domain.type.HiringStageResult
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull


data class CreateMemberJobSummaryCommand(

    val memberId: Long,

    val jobSummaryId: Long,

    val brandName: String,

    val positionName: String,

    val brandPositionName: String,

    val positionCategoryName: String
)

data class ChangeSaveTypeReq(

    @field:NotNull
    val saveType: MemberJobSummarySaveType
)
data class AddStageReq(

    @field:NotNull
    val stage: HiringStage,

    @field:NotBlank
    val note: String
)

data class UpdateStageReq(

    @field:NotNull
    val stage: HiringStage,

    @field:NotBlank
    val note: String,

    val result: HiringStageResult?
)

data class ExistsResponse(
    val exists: Boolean
)

data class AddCoverLetterReq(

    @field:NotBlank
    val question: String,

    @field:NotBlank
    val content: String,

    val sortOrder: Int? = null
)

data class UpdateCoverLetterReq(

    @field:NotBlank
    val question: String,

    @field:NotBlank
    val content: String,

    @field:NotNull
    val sortOrder: Int
)