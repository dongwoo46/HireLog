package com.hirelog.api.job.application.summary

import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.port.JobSummaryRequestCommand
import com.hirelog.api.job.domain.model.JobSummaryRequest
import com.hirelog.api.job.domain.type.JobSummaryRequestStatus
import com.hirelog.api.relation.application.memberjobsummary.MemberJobSummaryWriteService
import com.hirelog.api.relation.application.view.CreateMemberJobSummaryCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * JobSummaryRequest Write Service
 *
 * 책임:
 * - 요청 기록 생성
 * - 완료 시 요청 상태 업데이트 + MemberJobSummary 자동 생성
 * - 실패 시 요청 상태 업데이트
 *
 * 정책:
 * - requestId:memberId = 1:1 (요청마다 고유 UUID)
 * - 상태 전이만 담당, 알림(SSE)은 리스너에서 처리
 */
@Service
class JobSummaryRequestWriteService(
    private val jobSummaryRequestCommand: JobSummaryRequestCommand,
    private val memberJobSummaryWriteService: MemberJobSummaryWriteService
) {

    /**
     * 요청 기록 생성
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
     * @return 완료된 요청의 memberId (PENDING 요청이 없으면 null)
     *
     * 트랜잭션 정책:
     * - AFTER_COMMIT 리스너에서 호출 → 별도 트랜잭션
     * - 핵심 트랜잭션(JobSummary + Outbox + Processing)과 분리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun completeRequest(
        requestId: String,
        jobSummaryId: Long,
        brandName: String,
        positionName: String,
        brandPositionName: String,
        positionCategoryName: String
    ): Long? {

        log.info(
            "[COMPLETE_REQUEST_START] requestId={}, jobSummaryId={}, thread={}, txActive={}, txName={}",
            requestId, jobSummaryId,
            Thread.currentThread().name,
            TransactionSynchronizationManager.isActualTransactionActive(),
            TransactionSynchronizationManager.getCurrentTransactionName()
        )

        val request = jobSummaryRequestCommand.findByRequestIdAndStatus(
            requestId = requestId,
            status = JobSummaryRequestStatus.PENDING
        )

        if (request == null) {
            log.warn(
                "[COMPLETE_REQUEST_NOT_FOUND] requestId={}, status=PENDING → 해당 요청 없음",
                requestId
            )
            return null
        }

        log.info(
            "[COMPLETE_REQUEST_FOUND] requestId={}, memberId={}, currentStatus={}",
            requestId, request.memberId, request.status
        )

        request.complete(jobSummaryId)

        log.info(
            "[COMPLETE_REQUEST_AFTER_COMPLETE] requestId={}, newStatus={}, jobSummaryId={}",
            requestId, request.status, jobSummaryId
        )

        jobSummaryRequestCommand.save(request)

        log.info("[COMPLETE_REQUEST_SAVED] requestId={}", requestId)

        memberJobSummaryWriteService.save(
            CreateMemberJobSummaryCommand(
                memberId = request.memberId,
                jobSummaryId = jobSummaryId,
                brandName = brandName,
                positionName = positionName,
                brandPositionName = brandPositionName,
                positionCategoryName = positionCategoryName
            )
        )

        log.info(
            "[COMPLETE_REQUEST_DONE] requestId={}, memberId={}, jobSummaryId={}",
            requestId, request.memberId, jobSummaryId
        )

        return request.memberId
    }

    /**
     * 요청 실패 처리
     *
     * @return 실패 처리된 요청의 memberId (PENDING 요청이 없으면 null)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun failRequest(requestId: String): Long? {
        val request = jobSummaryRequestCommand.findByRequestIdAndStatus(
            requestId = requestId,
            status = JobSummaryRequestStatus.PENDING
        )

        if (request == null) {
            log.info("[JOB_SUMMARY_REQUEST_NO_PENDING] requestId={}", requestId)
            return null
        }

        request.markFailed()
        jobSummaryRequestCommand.save(request)

        log.info(
            "[JOB_SUMMARY_REQUEST_FAILED] requestId={}, memberId={}",
            requestId, request.memberId
        )

        return request.memberId
    }
}
