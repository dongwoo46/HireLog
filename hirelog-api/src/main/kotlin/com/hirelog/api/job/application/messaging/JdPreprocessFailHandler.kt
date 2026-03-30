package com.hirelog.api.job.application.messaging

import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.jobsummaryprocessing.JdSummaryProcessingWriteService
import com.hirelog.api.job.application.summary.port.JobSummaryRequestCommand
import com.hirelog.api.job.domain.type.JobSummaryRequestStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * JdPreprocessFailHandler
 *
 * 책임:
 * - Python 파이프라인 전처리 실패 시 도메인 상태 일괄 변경
 *
 * 트랜잭션 정책:
 * - JdSummaryProcessing + JobSummaryRequest 상태 변경을 하나의 트랜잭션에서 원자적 처리
 * - 하나만 FAILED로 변경되고 다른 하나는 PENDING으로 남는 상태 불일치 방지
 */
@Service
class JdPreprocessFailHandler(
    private val processingWriteService: JdSummaryProcessingWriteService,
    private val jobSummaryRequestCommand: JobSummaryRequestCommand
) {

    @Transactional
    fun handle(event: JdPreprocessFailEvent) {
        val processingId = UUID.fromString(event.requestId)

        // 1. JdSummaryProcessing → FAILED
        processingWriteService.markFailed(
            processingId = processingId,
            errorCode = event.errorCode,
            errorMessage = event.errorMessage
        )

        // 2. JobSummaryRequest → FAILED
        val request = jobSummaryRequestCommand.findByRequestIdAndStatus(
            requestId = event.requestId,
            status = JobSummaryRequestStatus.PENDING
        )

        if (request != null) {
            request.markFailed()
            jobSummaryRequestCommand.save(request)
        } else {
            log.error(
                "[JD_PREPROCESS_FAIL_REQUEST_NOT_FOUND] requestId={} - PENDING 상태 없음, 이미 상태 전이됐거나 데이터 불일치",
                event.requestId
            )
        }

        log.info(
            "[JD_PREPROCESS_FAIL_HANDLED] requestId={}, errorCode={}, errorCategory={}",
            event.requestId, event.errorCode, event.errorCategory
        )
    }
}
