package com.hirelog.api.job.application.messaging

import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.jobsummaryprocessing.JdSummaryProcessingWriteService
import com.hirelog.api.job.application.summary.event.JobSummaryRequestEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * JdPreprocessFailHandler
 *
 * 책임:
 * - Python 파이프라인 전처리 실패 시 JdSummaryProcessing 상태 변경
 * - JobSummaryRequest 상태 전이 + 알림 + SSE는 이벤트 발행으로 JobSummaryLifecycleListener에 위임
 *
 * 트랜잭션 정책:
 * - JdSummaryProcessing 상태 변경은 현재 트랜잭션에서 처리
 * - 이벤트는 AFTER_COMMIT 후 리스너에서 처리 (LLM 실패 경로와 동일한 패턴)
 */
@Service
class JdPreprocessFailHandler(
    private val processingWriteService: JdSummaryProcessingWriteService,
    private val eventPublisher: ApplicationEventPublisher
) {

    @Transactional
    fun handle(event: JdPreprocessFailEvent) {
        val processingId = UUID.fromString(event.requestId)

        // 1. JdSummaryProcessing → FAILED
        val processing = processingWriteService.markFailed(
            processingId = processingId,
            errorCode = event.errorCode,
            errorMessage = event.errorMessage
        )

        // 2. JobSummaryRequest 상태 전이 + 알림 + SSE는 리스너에 위임
        // JobSummaryLifecycleListener.onFailed() → failRequest() + notification + SSE
        // AFTER_COMMIT 보장: 현재 트랜잭션 커밋 후 리스너 실행
        eventPublisher.publishEvent(
            JobSummaryRequestEvent.Failed.of(
                processingId = event.requestId,
                errorCode = event.errorCode,
                requestId = event.requestId,
                brandName = processing?.commandBrandName,
                positionName = processing?.commandPositionName
            )
        )

        log.info(
            "[JD_PREPROCESS_FAIL_HANDLED] requestId={}, errorCode={}, errorCategory={}",
            event.requestId, event.errorCode, event.errorCategory
        )
    }
}
