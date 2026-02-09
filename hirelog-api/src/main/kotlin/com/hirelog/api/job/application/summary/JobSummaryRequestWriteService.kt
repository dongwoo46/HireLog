package com.hirelog.api.job.application.summary

import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.port.JobSummaryRequestCommand
import com.hirelog.api.job.domain.model.JobSummary
import com.hirelog.api.job.domain.model.JobSummaryRequest
import com.hirelog.api.job.domain.type.JobSummaryRequestStatus
import com.hirelog.api.relation.application.memberjobsummary.MemberJobSummaryWriteService
import com.hirelog.api.relation.application.view.CreateMemberJobSummaryCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * JobSummaryRequest Write Service
 *
 * 책임:
 * - 요청 기록 생성
 * - 완료 시 요청 상태 업데이트 + MemberJobSummary 자동 생성
 * - 실패 시 요청 상태 업데이트
 */
@Service
class JobSummaryRequestWriteService(
    private val jobSummaryRequestCommand: JobSummaryRequestCommand,
    private val memberJobSummaryWriteService: MemberJobSummaryWriteService
) {

    /**
     * 요청 기록 생성
     *
     * 정책:
     * - Controller에서 Kafka 전송 후 호출
     * - 초기 상태 PENDING
     */
    @Transactional
    fun createRequest(memberId: Long, requestId: String): JobSummaryRequest {
        val request = JobSummaryRequest.create(
            memberId = memberId,
            requestId = requestId
        )

        val saved = jobSummaryRequestCommand.save(request)

        log.info(
            "[JOB_SUMMARY_REQUEST_CREATED] memberId={}, requestId={}",
            memberId, requestId
        )

        return saved
    }

    /**
     * 요청 완료 처리 + MemberJobSummary 생성
     *
     * 트랜잭션 정책:
     * - JobSummaryCreationService.createWithOutbox() 트랜잭션에 참여
     * - JobSummaryRequest 완료 + MemberJobSummary 생성이 원자적으로 처리
     */
    fun completeRequests(requestId: String, summary: JobSummary) {
        val pendingRequests = jobSummaryRequestCommand.findAllByRequestIdAndStatus(
            requestId = requestId,
            status = JobSummaryRequestStatus.PENDING
        )

        if (pendingRequests.isEmpty()) {
            log.info("[JOB_SUMMARY_REQUEST_NO_PENDING] requestId={}", requestId)
            return
        }

        for (request in pendingRequests) {
            request.complete(summary.id)
            jobSummaryRequestCommand.save(request)

            memberJobSummaryWriteService.save(
                CreateMemberJobSummaryCommand(
                    memberId = request.memberId,
                    jobSummaryId = summary.id,
                    brandName = summary.brandName,
                    positionName = summary.positionName,
                    brandPositionName = summary.brandPositionName,
                    positionCategoryName = summary.positionCategoryName
                )
            )

            log.info(
                "[JOB_SUMMARY_REQUEST_COMPLETED] requestId={}, memberId={}, jobSummaryId={}",
                requestId, request.memberId, summary.id
            )
        }
    }

    /**
     * 요청 실패 처리
     *
     * 정책:
     * - PENDING 상태인 요청만 FAILED로 전이
     */
    @Transactional
    fun failRequests(requestId: String) {
        val pendingRequests = jobSummaryRequestCommand.findAllByRequestIdAndStatus(
            requestId = requestId,
            status = JobSummaryRequestStatus.PENDING
        )

        if (pendingRequests.isEmpty()) return

        for (request in pendingRequests) {
            request.markFailed()
            jobSummaryRequestCommand.save(request)

            log.info(
                "[JOB_SUMMARY_REQUEST_FAILED] requestId={}, memberId={}",
                requestId, request.memberId
            )
        }
    }
}
