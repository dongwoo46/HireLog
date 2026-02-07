package com.hirelog.api.relation.application.memberjobsummary

import com.hirelog.api.job.domain.HiringStage
import com.hirelog.api.relation.application.view.CreateMemberJobSummaryCommand
import com.hirelog.api.relation.domain.model.MemberJobSummary
import com.hirelog.api.relation.domain.type.HiringStageResult
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import org.springframework.stereotype.Service

/**
 * MemberJobSummary Write Service
 *
 * 책임:
 * - 쓰기 유스케이스 처리
 * - Aggregate 로드 및 도메인 로직 실행
 *
 * 규칙:
 * - Command Port만 사용
 * - Entity는 Service 내부에서만 사용
 */
@Service
class MemberJobSummaryWriteService(
    private val memberJobSummaryCommand: MemberJobSummaryCommand
) {

    fun save(command: CreateMemberJobSummaryCommand) {

        val exists = memberJobSummaryCommand.findEntityByMemberIdAndJobSummaryId(
            memberId = command.memberId,
            jobSummaryId = command.jobSummaryId
        )

        require(exists == null) {
            "MemberJobSummary already exists"
        }

        val summary = MemberJobSummary.create(
            memberId = command.memberId,
            jobSummaryId = command.jobSummaryId,
            brandName = command.brandName,
            positionName = command.positionName,
            brandPositionName = command.brandPositionName,
            positionCategoryName = command.positionCategoryName
        )

        memberJobSummaryCommand.save(summary)
    }

    fun unsave(
        memberId: Long,
        jobSummaryId: Long
    ) {
        val summary = memberJobSummaryCommand.findEntityByMemberIdAndJobSummaryId(
            memberId = memberId,
            jobSummaryId = jobSummaryId
        ) ?: return   // Idempotent

        summary.changeStatus(MemberJobSummarySaveType.UNSAVED)
        memberJobSummaryCommand.save(summary)
    }

    fun changeSaveType(
        memberId: Long,
        jobSummaryId: Long,
        saveType: MemberJobSummarySaveType
    ) {
        val summary = getOrThrow(memberId, jobSummaryId)
        summary.changeStatus(saveType)
        memberJobSummaryCommand.save(summary)
    }

    fun addStage(
        memberId: Long,
        jobSummaryId: Long,
        stage: HiringStage,
        note: String
    ) {
        val summary = getOrThrow(memberId, jobSummaryId)
        summary.addStageRecord(stage, note)
        memberJobSummaryCommand.save(summary)
    }

    fun updateStage(
        memberId: Long,
        jobSummaryId: Long,
        stage: HiringStage,
        note: String,
        result: HiringStageResult?
    ) {
        val summary = getOrThrow(memberId, jobSummaryId)
        summary.updateStage(stage, note, result)
        memberJobSummaryCommand.save(summary)
    }

    fun removeStage(
        memberId: Long,
        jobSummaryId: Long,
        stage: HiringStage
    ) {
        val summary = getOrThrow(memberId, jobSummaryId)
        summary.removeStageRecord(stage)
        memberJobSummaryCommand.save(summary)
    }

    private fun getOrThrow(
        memberId: Long,
        jobSummaryId: Long
    ): MemberJobSummary {
        return memberJobSummaryCommand.findEntityByMemberIdAndJobSummaryId(
            memberId = memberId,
            jobSummaryId = jobSummaryId
        ) ?: throw IllegalStateException(
            "MemberJobSummary not found (memberId=$memberId, jobSummaryId=$jobSummaryId)"
        )
    }
}
