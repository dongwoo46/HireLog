package com.hirelog.api.job.application.summary.event

import com.hirelog.api.common.application.sse.SseEmitterManager
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.JobSummaryRequestWriteService
import com.hirelog.api.notification.application.NotificationWriteService
import com.hirelog.api.notification.domain.type.NotificationReferenceType
import com.hirelog.api.notification.domain.type.NotificationType
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * JobSummary 파이프라인 이벤트 리스너
 *
 * 책임:
 * - 원본 트랜잭션 커밋 후 부가 작업 수행
 * - JobSummaryRequest 상태 전이 (별도 트랜잭션)
 * - Notification DB 저장(알림 영속화)
 * - SSE 실시간 알림 전송
 *
 * 설계 장치:
 * - 코어 서비스(JobSummaryWriteService)는 상태 전이만 담당
 * - 알림/부가 작업은 본 리스너에서 처리
 * - 리스너 실패가 원본 비즈니스에 영향 없음
 */
@Component
class JobSummaryLifecycleListener(
    private val jobSummaryRequestWriteService: JobSummaryRequestWriteService,
    private val notificationWriteService: NotificationWriteService,
    private val sseEmitterManager: SseEmitterManager
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onCompleted(event: JobSummaryRequestEvent.Completed) {
        MDC.put("requestId", event.requestId)
        try {
            log.info(
                "[JOB_SUMMARY_LIFECYCLE_COMPLETED] processingId={}, jobSummaryId={}, thread={}",
                event.processingId, event.jobSummaryId, Thread.currentThread().name
            )

            val memberId = completeRequest(event) ?: return
            val positionLabel = event.brandPositionName.takeIf { it.isNotBlank() } ?: event.positionName
            val sseData = mapOf(
                "requestId" to event.processingId,
                "jobSummaryId" to event.jobSummaryId,
                "brandName" to event.brandName,
                "positionName" to event.positionName,
                "brandPositionName" to event.brandPositionName,
                "positionCategoryName" to event.positionCategoryName
            )

            saveNotification {
                notificationWriteService.create(
                    memberId = memberId,
                    type = NotificationType.JOB_SUMMARY_COMPLETED,
                    title = "${event.brandName} ${positionLabel} 분석 완료",
                    message = "요청하신 채용공고(${positionLabel}) 분석이 완료되었습니다.",
                    referenceType = NotificationReferenceType.JOB_SUMMARY,
                    referenceId = event.jobSummaryId,
                    metadata = mapOf(
                        "requestId" to event.processingId,
                        "brandName" to event.brandName,
                        "positionName" to event.positionName,
                        "brandPositionName" to event.brandPositionName,
                        "positionCategoryName" to event.positionCategoryName
                    )
                )
            }

            sendSse(memberId, "JOB_SUMMARY_COMPLETED", sseData)
        } finally {
            MDC.clear()
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onFailed(event: JobSummaryRequestEvent.Failed) {
        MDC.put("requestId", event.requestId)
        try {
            log.warn(
                "[JOB_SUMMARY_LIFECYCLE_FAILED] processingId={}, errorCode={}, retryable={}, brandName={}, positionName={}",
                event.processingId, event.errorCode, event.retryable, event.brandName, event.positionName
            )

            val memberId = failRequest(event) ?: return
            val sseData = mapOf(
                "requestId" to event.processingId,
                "errorCode" to event.errorCode,
                "retryable" to event.retryable
            )

            saveNotification {
                notificationWriteService.create(
                    memberId = memberId,
                    type = NotificationType.JOB_SUMMARY_FAILED,
                    title = if (!event.brandName.isNullOrBlank() && !event.positionName.isNullOrBlank())
                        "${event.brandName} ${event.positionName} 분석 실패"
                    else
                        "채용공고 분석 실패",
                    message = "요청하신 채용공고 분석이 실패했습니다.",
                    metadata = mapOf(
                        "requestId" to event.processingId,
                        "errorCode" to event.errorCode,
                        "retryable" to event.retryable
                    )
                )
            }

            sendSse(memberId, "JOB_SUMMARY_FAILED", sseData)
        } finally {
            MDC.clear()
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onDuplicate(event: JobSummaryRequestEvent.Duplicate) {
        MDC.put("requestId", event.requestId)
        try {
            log.info(
                "[JOB_SUMMARY_LIFECYCLE_DUPLICATE] processingId={}, reason={}, brandName={}, positionName={}",
                event.processingId, event.reason, event.brandName, event.positionName
            )

            val memberId = duplicateRequest(event) ?: return
            val sseData = mapOf(
                "requestId" to event.processingId,
                "reason" to event.reason
            )

            saveNotification {
                notificationWriteService.create(
                    memberId = memberId,
                    type = NotificationType.JOB_SUMMARY_DUPLICATE,
                    title = if (!event.brandName.isNullOrBlank() && !event.positionName.isNullOrBlank())
                        "${event.brandName} ${event.positionName} 중복 요청"
                    else
                        "채용공고 중복 요청",
                    message = "동일한 공고가 이미 등록되어 중복 요청으로 처리되었습니다.",
                    metadata = mapOf(
                        "requestId" to event.processingId,
                        "reason" to event.reason
                    )
                )
            }

            sendSse(memberId, "JOB_SUMMARY_DUPLICATE", sseData)
        } finally {
            MDC.clear()
        }
    }

    private fun completeRequest(event: JobSummaryRequestEvent.Completed): Long? {
        return try {
            val memberId = jobSummaryRequestWriteService.completeRequest(
                requestId = event.processingId,
                jobSummaryId = event.jobSummaryId,
                brandName = event.brandName,
                positionName = event.positionName,
                brandPositionName = event.brandPositionName,
                positionCategoryName = event.positionCategoryName
            )

            if (memberId == null) {
                log.warn(
                    "[JOB_SUMMARY_LIFECYCLE_COMPLETED_NO_MEMBER] processingId={}",
                    event.processingId
                )
            }

            memberId
        } catch (e: Exception) {
            log.error(
                "[JOB_SUMMARY_LIFECYCLE_COMPLETED_FAILED] processingId={}, error={}",
                event.processingId, e.message, e
            )
            null
        }
    }

    private fun failRequest(event: JobSummaryRequestEvent.Failed): Long? {
        return try {
            jobSummaryRequestWriteService.failRequest(
                requestId = event.processingId
            )
        } catch (e: Exception) {
            log.error(
                "[JOB_SUMMARY_LIFECYCLE_FAILED_ERROR] processingId={}, error={}",
                event.processingId, e.message, e
            )
            null
        }
    }

    private fun duplicateRequest(event: JobSummaryRequestEvent.Duplicate): Long? {
        return try {
            jobSummaryRequestWriteService.duplicateRequest(
                requestId = event.processingId
            )
        } catch (e: Exception) {
            log.error(
                "[JOB_SUMMARY_LIFECYCLE_DUPLICATE_ERROR] processingId={}, error={}",
                event.processingId, e.message, e
            )
            null
        }
    }

    private fun saveNotification(action: () -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            log.error("[NOTIFICATION_CREATE_FAILED] error={}", e.message, e)
        }
    }

    private fun sendSse(memberId: Long, eventName: String, data: Any) {
        try {
            sseEmitterManager.send(memberId = memberId, eventName = eventName, data = data)
        } catch (e: Exception) {
            log.error("[SSE_SEND_FAILED] memberId={}, event={}, error={}", memberId, eventName, e.message, e)
        }
    }
}
