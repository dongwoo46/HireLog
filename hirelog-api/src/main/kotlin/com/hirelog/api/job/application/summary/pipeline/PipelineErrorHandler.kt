package com.hirelog.api.job.application.summary.pipeline

import com.hirelog.api.common.exception.GeminiCallException
import com.hirelog.api.common.exception.GeminiParseException
import com.hirelog.api.job.application.jobsummaryprocessing.JdSummaryProcessingWriteService
import com.hirelog.api.job.application.summary.event.JobSummaryRequestEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeoutException

@Service
class PipelineErrorHandler(
    private val processingWriteService: JdSummaryProcessingWriteService,
    private val eventPublisher: ApplicationEventPublisher
) {

    fun handle(processingId: UUID, ex: Throwable, requestId: String, phase: String) {
        val cause = if (ex is CompletionException) ex.cause ?: ex else ex

        val errorCode = when (cause) {
            is GeminiCallException -> "LLM_CALL_FAILED"
            is GeminiParseException -> "LLM_PARSE_FAILED"
            is TimeoutException -> "LLM_TIMEOUT"
            else -> "FAILED_AT_$phase"
        }

        processingWriteService.markFailed(
            processingId,
            errorCode,
            cause.message ?: "Unknown error"
        )

        eventPublisher.publishEvent(
            JobSummaryRequestEvent.Failed.of(processingId.toString(), errorCode)
        )
    }
}
