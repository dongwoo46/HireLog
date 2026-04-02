package com.hirelog.api.job.application.summary

import com.fasterxml.jackson.databind.ObjectMapper
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.jdsummaryprocessing.port.JdSummaryProcessingQuery
import com.hirelog.api.job.application.jobsummaryprocessing.JdSummaryProcessingWriteService
import com.hirelog.api.job.application.summary.command.JobSummaryGenerateCommand
import com.hirelog.api.job.application.summary.pipeline.LlmInvocationService
import com.hirelog.api.job.application.summary.pipeline.PipelineErrorHandler
import com.hirelog.api.job.application.summary.pipeline.PostLlmProcessor
import com.hirelog.api.job.application.summary.pipeline.PreLlmProcessor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.*

/**
 * SummaryGenerationPipeline
 *
 * 책임:
 * - JD 요약 생성 유스케이스의 순수 비즈니스 파이프라인 실행
 *
 * 설계 원칙:
 * - Kafka / Redis / ACK / Executor 개념 ❌
 * - Application Command만 입력으로 받는다
 */
@Service
class JdSummaryGenerationFacade(
    private val preLlm: PreLlmProcessor,
    private val llmInvoker: LlmInvocationService,
    private val postLlm: PostLlmProcessor,
    private val errorHandler: PipelineErrorHandler,
    private val processingWriteService: JdSummaryProcessingWriteService,
    private val processingQuery: JdSummaryProcessingQuery,
    private val objectMapper: ObjectMapper,
    @Qualifier("blockingExecutor") private val executor: Executor
) {

    fun execute(command: JobSummaryGenerateCommand): CompletableFuture<Void> {
        log.info(
            "[PIPELINE_START] requestId={}, brandName={}, positionName={}, source={}",
            command.requestId, command.brandName, command.positionName, command.source
        )

        val processing = processingQuery.findById(UUID.fromString(command.requestId))
            ?: throw IllegalStateException("JdSummaryProcessing not found for requestId=${command.requestId}")

        val preResult = try {
            preLlm.execute(processing.id, command) ?: run {
                log.info("[PIPELINE_PRE_LLM_SKIPPED] requestId={}", command.requestId)
                return CompletableFuture.completedFuture(null)
            }
        } catch (e: Exception) {
            errorHandler.handle(processing.id, e, command.requestId, "PRE_LLM")
            return CompletableFuture.completedFuture(null)
        }

        return llmInvoker
            .invoke(command, preResult.positionCandidates, preResult.existCompanies)
            .thenAcceptAsync({ llmResult ->
                processingWriteService.saveLlmResult(
                    processing.id,
                    objectMapper.writeValueAsString(llmResult),
                    command.brandName,
                    command.positionName
                )
                try {
                    postLlm.execute(preResult.snapshotId, llmResult, processing.id, command)
                } catch (e: Exception) {
                    errorHandler.handlePostLlm(processing.id, e, command.requestId)
                }
            }, executor)
            .exceptionally {
                errorHandler.handle(processing.id, it, command.requestId, "LLM")
                null
            }
    }
}

