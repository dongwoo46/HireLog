package com.hirelog.api.job.presentation.controller

import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.common.config.security.CurrentUser
import com.hirelog.api.job.application.intake.JdIntakeService
import com.hirelog.api.job.application.summary.JobSummaryWriteService
import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.job.application.summary.query.JobSummarySearchCondition
import com.hirelog.api.job.application.summary.query.JobSummarySearchResult
import com.hirelog.api.job.application.summary.view.JobSummaryDetailView
import com.hirelog.api.job.application.summary.view.JobSummaryView
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryOpenSearchQuery
import com.hirelog.api.job.presentation.controller.dto.request.JobSummarySearchReq
import com.hirelog.api.job.presentation.controller.dto.request.JobSummaryOcrReq
import com.hirelog.api.job.presentation.controller.dto.request.JobSummaryTextReq
import com.hirelog.api.job.presentation.controller.dto.request.JobSummaryUrlReq
import com.hirelog.api.job.presentation.controller.dto.response.JobSummaryUrlRes
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/job-summary")
class JobSummaryController(
    private val jobSummaryQuery: JobSummaryQuery,
    private val jdIntakeService: JdIntakeService,
    private val openSearchQuery: JobSummaryOpenSearchQuery,
    private val jobSummaryWriteService: JobSummaryWriteService
) {

    /**
     * JobSummary OpenSearch 검색
     *
     * 검색 기능:
     * - 키워드 검색 (한글/영어)
     * - 필터: careerType, brandId, companyId, positionId, techStacks
     * - 정렬: 최신순, 오래된순, 관련도순
     * - 페이징
     */
    @GetMapping("/search")
    fun search(request: JobSummarySearchReq): ResponseEntity<JobSummarySearchResult> {
        val query = request.toQuery()
        val result = openSearchQuery.search(query)
        return ResponseEntity.ok(result)
    }


    /**
     * JobSummary 상세 조회
     *
     * 응답:
     * - JobSummary 전체 필드 + Insight + Reviews
     * - 현재 사용자의 저장 상태 (memberJobSummaryId, memberSaveType)
     * - 비활성화된 JobSummary는 조회 불가
     */
    @GetMapping("/{id}")
    fun getDetail(
        @PathVariable id: Long,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<JobSummaryDetailView> {
        val detail = jobSummaryQuery.findDetailById(id, member.memberId)
            ?: throw IllegalArgumentException("JobSummary not found: $id")
        return ResponseEntity.ok(detail)
    }

    /**
     * JD 텍스트 요약 요청
     *
     * 처리 방식:
     * - 비동기 파이프라인 진입
     * - 즉시 200 반환
     *
     * 주의:
     * - LLM 벤더는 내부 구현
     */
    @PostMapping("/text")
    fun requestTextSummary(
        @Valid @RequestBody request: JobSummaryTextReq,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<Void> {

        jdIntakeService.requestText(
            memberId = member.memberId,
            brandName = request.brandName,
            brandPositionName = request.brandPositionName,
            text = request.jdText,
        )

        return ResponseEntity.ok().build()
    }

    /**
     * OCR 기반 JD 요약 요청
     *
     * 처리 방식:
     * - 이미지 파일 저장 후 비동기 파이프라인 진입
     * - 즉시 200 반환
     */
    @PostMapping(
        value = ["/ocr"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun requestOcrSummary(
        @ModelAttribute @Valid request: JobSummaryOcrReq,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<Map<String, String>> {

        val requestId = jdIntakeService.requestOcr(
            memberId = member.memberId,
            brandName = request.brandName,
            brandPositionName = request.brandPositionName,
            imageFiles = request.images,
        )

        return ResponseEntity.ok(mapOf("requestId" to requestId))
    }

    /**
     * URL 기반 JD 요약 요청
     *
     * 처리 방식:
     * - 중복 체크 후 기존 JobSummary 있으면 즉시 반환
     * - 신규면 비동기 파이프라인에 전달
     */
    @PostMapping("/url")
    fun requestUrlSummary(
        @Valid @RequestBody request: JobSummaryUrlReq,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<JobSummaryUrlRes> {

        // 중복 체크: 동일 URL로 생성된 JobSummary 존재 여부
        val existingSummary = jobSummaryQuery.findBySourceUrl(request.url)
        if (existingSummary != null) {
            return ResponseEntity.ok(JobSummaryUrlRes.duplicateOf(existingSummary))
        }

        val requestId = jdIntakeService.requestUrl(
            memberId = member.memberId,
            brandName = request.brandName,
            brandPositionName = request.brandPositionName,
            url = request.url,
        )

        return ResponseEntity.ok(JobSummaryUrlRes.newRequest(requestId))
    }

    /**
     * JobSummary 비활성화 (admin 전용)
     *
     * 처리 방식:
     * - isActive = false 설정
     * - Outbox 이벤트 발행 → OpenSearch 문서 삭제
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    fun deactivate(@PathVariable id: Long): ResponseEntity<Void> {
        jobSummaryWriteService.deactivate(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * JobSummary 재활성화 (admin 전용)
     *
     * 처리 방식:
     * - isActive = true 설정
     * - Outbox 이벤트 발행 → OpenSearch 문서 재인덱싱
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    fun activate(@PathVariable id: Long): ResponseEntity<Void> {
        jobSummaryWriteService.activate(id)
        return ResponseEntity.noContent().build()
    }
}
