package com.hirelog.api.relation.application.memberjobsummary

import com.hirelog.api.job.domain.type.HiringStage
import com.hirelog.api.job.application.summary.port.JobSummaryCommand
import com.hirelog.api.relation.application.view.CreateMemberJobSummaryCommand
import com.hirelog.api.relation.domain.model.MemberJobSummary
import com.hirelog.api.relation.domain.type.HiringStageResult
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
@Transactional
class MemberJobSummaryWriteService(
    private val memberJobSummaryCommand: MemberJobSummaryCommand,
    private val jobSummaryCommand: JobSummaryCommand
) {

    fun save(command: CreateMemberJobSummaryCommand) {

        val exists = memberJobSummaryCommand.findEntityByMemberIdAndJobSummaryId(
            memberId = command.memberId,
            jobSummaryId = command.jobSummaryId
        )

        if (exists != null) {
            if (exists.saveType == MemberJobSummarySaveType.UNSAVED) {
                exists.restoreFromArchived()
            } else {
                exists.changeStatus(MemberJobSummarySaveType.SAVED)
            }
            memberJobSummaryCommand.save(exists)
            return
        }

        val summary = createFromJobSummary(command.memberId, command.jobSummaryId)
        memberJobSummaryCommand.save(summary)
    }

    fun changeSaveType(
        memberId: Long,
        jobSummaryId: Long,
        saveType: MemberJobSummarySaveType
    ) {
        val summary = getOrCreateForSaveType(memberId, jobSummaryId, saveType) ?: return
        summary.changeStatus(saveType)
        memberJobSummaryCommand.save(summary)
    }

    fun addStage(
        memberId: Long,
        jobSummaryId: Long,
        stage: HiringStage,
        note: String,
        result: HiringStageResult? = null
    ) {
        val summary = getOrCreateForStage(memberId, jobSummaryId)
        if (summary.saveType == MemberJobSummarySaveType.UNSAVED) {
            summary.restoreFromArchived()
        }
        if (summary.saveType != MemberJobSummarySaveType.APPLY) {
            summary.changeStatus(MemberJobSummarySaveType.APPLY)
        }
        summary.addStageRecord(stage, note, result)
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

    fun addCoverLetter(
        memberId: Long,
        jobSummaryId: Long,
        question: String,
        content: String,
        sortOrder: Int?
    ) {
        val summary = getOrThrow(memberId, jobSummaryId)
        if (sortOrder != null) {
            summary.addCoverLetter(question, content, sortOrder)
        } else {
            summary.addCoverLetter(question, content)
        }
        memberJobSummaryCommand.save(summary)
    }

    fun updateCoverLetter(
        memberId: Long,
        jobSummaryId: Long,
        coverLetterId: Long,
        question: String,
        content: String,
        sortOrder: Int
    ) {
        val summary = getOrThrow(memberId, jobSummaryId)
        summary.updateCoverLetter(coverLetterId, question, content, sortOrder)
        memberJobSummaryCommand.save(summary)
    }

    fun removeCoverLetter(
        memberId: Long,
        jobSummaryId: Long,
        coverLetterId: Long
    ) {
        val summary = getOrThrow(memberId, jobSummaryId)
        summary.removeCoverLetter(coverLetterId)
        memberJobSummaryCommand.save(summary)
    }

    private fun getOrThrow(
        memberId: Long,
        jobSummaryId: Long
    ): MemberJobSummary {
        return memberJobSummaryCommand.findEntityByMemberIdAndJobSummaryId(
            memberId = memberId,
            jobSummaryId = jobSummaryId
        ) ?: throw IllegalArgumentException(
            "MemberJobSummary not found (memberId=$memberId, jobSummaryId=$jobSummaryId)"
        )
    }

    private fun getOrCreateForSaveType(
        memberId: Long,
        jobSummaryId: Long,
        saveType: MemberJobSummarySaveType
    ): MemberJobSummary? {
        val existing = memberJobSummaryCommand.findEntityByMemberIdAndJobSummaryId(
            memberId = memberId,
            jobSummaryId = jobSummaryId
        )
        if (existing != null) {
            return existing
        }

        // UNSAVED on a missing row is already the desired state (idempotent unsave).
        if (saveType == MemberJobSummarySaveType.UNSAVED) {
            return null
        }

        if (saveType == MemberJobSummarySaveType.APPLY) {
            throw IllegalArgumentException("APPLY is set only when preparation records are written")
        }

        return createFromJobSummary(memberId, jobSummaryId)
    }

    private fun getOrCreateForStage(
        memberId: Long,
        jobSummaryId: Long
    ): MemberJobSummary {
        return memberJobSummaryCommand.findEntityByMemberIdAndJobSummaryId(
            memberId = memberId,
            jobSummaryId = jobSummaryId
        ) ?: createFromJobSummary(memberId, jobSummaryId)
    }

    private fun createFromJobSummary(memberId: Long, jobSummaryId: Long): MemberJobSummary {
        val jobSummary = jobSummaryCommand.findById(jobSummaryId)
            ?: throw IllegalArgumentException("JobSummary not found: $jobSummaryId")

        return MemberJobSummary.create(
            memberId = memberId,
            jobSummaryId = jobSummary.id,
            brandName = jobSummary.brandName,
            positionName = jobSummary.positionName,
            brandPositionName = jobSummary.brandPositionName,
            positionCategoryName = jobSummary.positionCategoryName,
            careerType = jobSummary.careerType?.name,
            companyDomain = jobSummary.companyDomain?.name,
            companySize = jobSummary.companySize?.name,
        )
    }
}
