package com.hirelog.api.job.application.summary.pipeline

import com.hirelog.api.common.exception.GeminiCallException
import com.hirelog.api.common.exception.GeminiParseException
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.jobsummaryprocessing.JdSummaryProcessingWriteService
import com.hirelog.api.job.application.summary.SnapshotAlreadySummarizedException
import com.hirelog.api.job.application.summary.event.JobSummaryRequestEvent
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.ratelimiter.RequestNotPermitted
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
        val rootCause = unwrapDeep(cause)

        val errorCode = when (cause) {
            is GeminiCallException -> mapGeminiCallErrorCode(rootCause)
            is GeminiParseException -> "LLM_PARSE_FAILED"
            is TimeoutException -> "LLM_TIMEOUT"
            else -> "FAILED_AT_$phase"
        }

        log.error(
            "[PIPELINE_FAILED] processingId={}, phase={}, errorCode={}, errorClass={}, rootClass={}, error={}",
            processingId, phase, errorCode, cause::class.qualifiedName, rootCause::class.qualifiedName, cause.message, cause
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

    private fun mapGeminiCallErrorCode(rootCause: Throwable): String = when (rootCause) {
        is TimeoutException -> "LLM_TIMEOUT"
        is RequestNotPermitted -> "LLM_RATE_LIMITED"
        is CallNotPermittedException -> "LLM_CIRCUIT_OPEN"
        else -> "LLM_CALL_FAILED"
    }

    private fun unwrapDeep(error: Throwable): Throwable {
        var current = error
        while (current is CompletionException && current.cause != null) {
            current = current.cause!!
        }
        while (current is GeminiCallException && current.cause != null) {
            current = current.cause!!
        }
        return current
    }
}
