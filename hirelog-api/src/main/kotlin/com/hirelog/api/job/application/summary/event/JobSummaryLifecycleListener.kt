package com.hirelog.api.job.application.summary.event

import com.hirelog.api.common.application.sse.SseEmitterManager
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.JobSummaryRequestWriteService
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * JobSummary 파이프라인 이벤트 리스너
 *
 * 책임:
 * - 핵심 트랜잭션 커밋 후 부가 작업 실행
 * - JobSummaryRequest 상태 전이 (별도 트랜잭션)
 * - SSE 알림 전송
 *
 * 설계 원칙:
 * - 도메인 서비스(JobSummaryCreationService)는 상태 전이에만 집중
 * - 알림/부가 작업은 이 리스너에서 처리
 * - 리스너 실패가 핵심 비즈니스에 영향 없음
 */
@Component
class JobSummaryLifecycleListener(
    private val jobSummaryRequestWriteService: JobSummaryRequestWriteService,
    private val sseEmitterManager: SseEmitterManager
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onCompleted(event: JobSummaryRequestEvent.Completed) {
        log.info(
            "[JOB_SUMMARY_LIFECYCLE_COMPLETED] processingId={}, jobSummaryId={}",
            event.processingId, event.jobSummaryId
        )

        try {
            val memberId = jobSummaryRequestWriteService.completeRequest(
                requestId = event.processingId,
                jobSummaryId = event.jobSummaryId,
                brandName = event.brandName,
                positionName = event.positionName,
                brandPositionName = event.brandPositionName,
                positionCategoryName = event.positionCategoryName
            ) ?: return

            sseEmitterManager.send(
                memberId = memberId,
                eventName = "JOB_SUMMARY_COMPLETED",
                data = mapOf(
                    "requestId" to event.processingId,
                    "jobSummaryId" to event.jobSummaryId,
                    "brandName" to event.brandName,
                    "positionName" to event.positionName,
                    "brandPositionName" to event.brandPositionName,
                    "positionCategoryName" to event.positionCategoryName
                )
            )
        } catch (e: Exception) {
            log.error(
                "[JOB_SUMMARY_LIFECYCLE_COMPLETED_FAILED] processingId={}, error={}",
                event.processingId, e.message, e
            )
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onFailed(event: JobSummaryRequestEvent.Failed) {
        log.info(
            "[JOB_SUMMARY_LIFECYCLE_FAILED] processingId={}, errorCode={}, retryable={}",
            event.processingId, event.errorCode, event.retryable
        )

        try {
            val memberId = jobSummaryRequestWriteService.failRequest(
                requestId = event.processingId
            ) ?: return

            sseEmitterManager.send(
                memberId = memberId,
                eventName = "JOB_SUMMARY_FAILED",
                data = mapOf(
                    "requestId" to event.processingId,
                    "errorCode" to event.errorCode,
                    "retryable" to event.retryable
                )
            )
        } catch (e: Exception) {
            log.error(
                "[JOB_SUMMARY_LIFECYCLE_FAILED_ERROR] processingId={}, error={}",
                event.processingId, e.message, e
            )
        }
    }
}
