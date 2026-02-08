package com.hirelog.api.relation.application.view

import com.hirelog.api.job.domain.HiringStage
import com.hirelog.api.relation.domain.type.HiringStageResult
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime





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