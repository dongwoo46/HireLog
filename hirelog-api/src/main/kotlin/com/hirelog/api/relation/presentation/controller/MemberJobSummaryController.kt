package com.hirelog.api.relation.presentation.controller

import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.common.config.security.CurrentUser
import com.hirelog.api.relation.application.jobsummary.MemberJobSummaryWriteService
import com.hirelog.api.relation.application.jobsummary.query.MemberJobSummaryQuery
import com.hirelog.api.relation.application.jobsummary.view.SavedJobSummaryView
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import com.hirelog.api.relation.presentation.controller.dto.ChangeSaveTypeReq
import com.hirelog.api.relation.presentation.controller.dto.SaveJobSummaryReq
import com.hirelog.api.relation.presentation.controller.dto.UpdateMemoReq
import com.hirelog.api.common.application.port.PagedResult
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/member-job-summary")
class MemberJobSummaryController(
    private val memberJobSummaryWriteService: MemberJobSummaryWriteService,
    private val memberJobSummaryQuery: MemberJobSummaryQuery
) {

    /**
     * JD 저장
     *
     * 정책:
     * - 동일 member-jobSummary 조합은 단 하나만 존재
     * - 중복 저장 시 409 Conflict
     */
    @PostMapping
    fun save(
        @Valid @RequestBody request: SaveJobSummaryReq,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<Void> {

        memberJobSummaryWriteService.save(
            memberId = member.memberId,
            jobSummaryId = request.jobSummaryId,
            saveType = request.saveType,
            memo = request.memo
        )

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    /**
     * JD 저장 취소
     *
     * 정책:
     * - Idempotent delete
     * - 존재하지 않아도 204 반환
     */
    @DeleteMapping("/{jobSummaryId}")
    fun unsave(
        @PathVariable jobSummaryId: Long,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<Void> {

        memberJobSummaryWriteService.delete(
            memberId = member.memberId,
            jobSummaryId = jobSummaryId
        )

        return ResponseEntity.noContent().build()
    }

    /**
     * 저장 유형 변경
     *
     * 정책:
     * - SAVED ↔ APPLY 변경
     * - 존재하지 않으면 404
     */
    @PatchMapping("/save-type")
    fun changeSaveType(
        @Valid @RequestBody request: ChangeSaveTypeReq,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<Void> {

        memberJobSummaryWriteService.changeSaveType(
            memberId = member.memberId,
            jobSummaryId = request.jobSummaryId,
            saveType = request.saveType
        )

        return ResponseEntity.ok().build()
    }

    /**
     * 메모 수정
     *
     * 정책:
     * - null 전달 시 메모 삭제
     * - 존재하지 않으면 404
     */
    @PatchMapping("/memo")
    fun updateMemo(
        @Valid @RequestBody request: UpdateMemoReq,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<Void> {

        memberJobSummaryWriteService.updateMemo(
            memberId = member.memberId,
            jobSummaryId = request.jobSummaryId,
            memo = request.memo
        )

        return ResponseEntity.ok().build()
    }

    /**
     * 내가 저장한 JD 목록 조회
     *
     * 응답:
     * - MemberJobSummary + JobSummary 정보 포함
     * - 저장일 기준 내림차순
     */
    @GetMapping
    fun getSavedJobSummaries(
        @RequestParam(required = false) saveType: MemberJobSummarySaveType?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<PagedResult<SavedJobSummaryView>> {

        val result = memberJobSummaryQuery.findSavedJobSummaries(
            memberId = member.memberId,
            saveType = saveType,
            page = page,
            size = size
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

        val exists = memberJobSummaryQuery.existsByMemberIdAndJobSummaryId(
            memberId = member.memberId,
            jobSummaryId = jobSummaryId
        )

        return ResponseEntity.ok(mapOf("exists" to exists))
    }
}
