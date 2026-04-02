package com.hirelog.api.job.application.summary.pipeline

import com.hirelog.api.common.exception.GeminiCallException
import com.hirelog.api.common.exception.GeminiParseException
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.jobsummaryprocessing.JdSummaryProcessingWriteService
import com.hirelog.api.job.application.summary.SnapshotAlreadySummarizedException
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

        log.error(
            "[PIPELINE_FAILED] processingId={}, phase={}, errorCode={}, error={}",
            processingId, phase, errorCode, cause.message, cause
        )

        val processing = processingWriteService.markFailed(
            processingId,
            errorCode,
            cause.message ?: "Unknown error"
        )

        eventPublisher.publishEvent(
            JobSummaryRequestEvent.Failed.of(
                processingId.toString(), errorCode, requestId,
                brandName = processing?.commandBrandName,
                positionName = processing?.commandPositionName
            )
        )
    }

    fun handlePostLlm(processingId: UUID, ex: Throwable, requestId: String) {
        val cause = if (ex is CompletionException) ex.cause ?: ex else ex

        if (cause is SnapshotAlreadySummarizedException) {
            log.error(
                "[PIPELINE_POST_LLM_DUPLICATE] processingId={}, reason={}",
                processingId, cause.message
            )

            val processing = processingWriteService.markDuplicate(
                processingId = processingId,
                reason = "SNAPSHOT_SUMMARY_DUPLICATE"
            )

            eventPublisher.publishEvent(
                JobSummaryRequestEvent.Duplicate(
                    requestId = requestId,
                    processingId = processingId.toString(),
                    reason = "SNAPSHOT_SUMMARY_DUPLICATE",
                    brandName = processing.commandBrandName,
                    positionName = processing.commandPositionName
                )
            )
            return
        }

        log.error(
            "[PIPELINE_POST_LLM_FAILED] processingId={}, error={}",
            processingId, cause.message, cause
        )

        val processing = processingWriteService.markPostLlmFailed(
            processingId,
            "POST_LLM_FAILED",
            cause.message ?: "Unknown error"
        )

        eventPublisher.publishEvent(
            JobSummaryRequestEvent.Failed.of(
                processingId.toString(), "POST_LLM_FAILED", requestId,
                brandName = processing.commandBrandName,
                positionName = processing.commandPositionName
            )
        )
    }
}
