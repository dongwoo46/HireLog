package com.hirelog.api.relation.presentation.controller

import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.common.config.security.CurrentUser
import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.job.domain.type.HiringStage
import com.hirelog.api.relation.application.memberjobsummary.MemberJobSummaryReadService
import com.hirelog.api.relation.application.memberjobsummary.MemberJobSummaryWriteService
import com.hirelog.api.relation.application.memberjobsummary.view.MemberJobSummaryDetailView
import com.hirelog.api.relation.application.memberjobsummary.view.MemberJobSummaryListView
import com.hirelog.api.relation.application.view.*

import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/member-job-summaries")
class MemberJobSummaryController(
    private val writeService: MemberJobSummaryWriteService,
    private val readService: MemberJobSummaryReadService
) {

    /**
     * JD 저장
     *
     * 정책:
     * - member + jobSummary 조합은 유일
     * - 중복 저장 시 409
     */
    @PostMapping
    fun save(
        @Valid @RequestBody request: CreateMemberJobSummaryCommand,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<Void> {

        writeService.save(
            CreateMemberJobSummaryCommand(
                memberId = member.memberId,
                jobSummaryId = request.jobSummaryId,
                brandName = request.brandName,
                positionName = request.positionName,
                brandPositionName = request.brandPositionName,
                positionCategoryName = request.positionCategoryName
            )
        )

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    /**
     * JD 저장 해제
     *
     * 정책:
     * - Idempotent
     * - 존재하지 않아도 204
     */
    @DeleteMapping("/{jobSummaryId}")
    fun unsave(
        @PathVariable jobSummaryId: Long,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<Void> {

        writeService.unsave(
            memberId = member.memberId,
            jobSummaryId = jobSummaryId
        )

        return ResponseEntity.noContent().build()
    }

    /**
     * 저장 상태 변경
     *
     * 정책:
     * - SAVED ↔ APPLY
     */
    @PatchMapping("/{jobSummaryId}/save-type")
    fun changeSaveType(
        @PathVariable jobSummaryId: Long,
        @Valid @RequestBody request: ChangeSaveTypeReq,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<Void> {

        writeService.changeSaveType(
            memberId = member.memberId,
            jobSummaryId = jobSummaryId,
            saveType = request.saveType
        )

        return ResponseEntity.ok().build()
    }

    /**
     * 채용 단계 추가
     */
    @PostMapping("/{jobSummaryId}/stages")
    fun addStage(
        @PathVariable jobSummaryId: Long,
        @Valid @RequestBody request: AddStageReq,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<Void> {

        writeService.addStage(
            memberId = member.memberId,
            jobSummaryId = jobSummaryId,
            stage = request.stage,
            note = request.note
        )

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    /**
     * 채용 단계 수정
     */
    @PatchMapping("/{jobSummaryId}/stages")
    fun updateStage(
        @PathVariable jobSummaryId: Long,
        @Valid @RequestBody request: UpdateStageReq,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<Void> {

        writeService.updateStage(
            memberId = member.memberId,
            jobSummaryId = jobSummaryId,
            stage = request.stage,
            note = request.note,
            result = request.result
        )

        return ResponseEntity.ok().build()
    }

    /**
     * 채용 단계 삭제
     */
    @DeleteMapping("/{jobSummaryId}/stages/{stage}")
    fun removeStage(
        @PathVariable jobSummaryId: Long,
        @PathVariable stage: HiringStage,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<Void> {

        writeService.removeStage(
            memberId = member.memberId,
            jobSummaryId = jobSummaryId,
            stage = stage
        )

        return ResponseEntity.noContent().build()
    }

    /**
     * 내가 저장한 JD 목록 조회
     */
    @GetMapping
    fun getMySummaries(
        @RequestParam(required = false) saveType: MemberJobSummarySaveType?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<PagedResult<MemberJobSummaryListView>> {

        val result = readService.getMySummaries(
            memberId = member.memberId,
            saveType = saveType,
            page = page,
            size = size
        )

        return ResponseEntity.ok(result)
    }

    /**
     * 저장 JD 상세 조회
     */
    @GetMapping("/{jobSummaryId}")
    fun getDetail(
        @PathVariable jobSummaryId: Long,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<MemberJobSummaryDetailView> {

        val result = readService.getDetail(
            memberId = member.memberId,
            jobSummaryId = jobSummaryId
        )

        return ResponseEntity.ok(result)
    }

    /**
     * 특정 JD 저장 여부 확인
     */
    @GetMapping("/{jobSummaryId}/exists")
    fun exists(
        @PathVariable jobSummaryId: Long,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<Map<String, Boolean>> {

        val exists = readService.exists(
            memberId = member.memberId,
            jobSummaryId = jobSummaryId
        )

        return ResponseEntity.ok(mapOf("exists" to exists))
    }
}
