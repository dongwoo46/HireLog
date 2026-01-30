package com.hirelog.api.job.application.summary

import com.hirelog.api.job.application.summary.command.JobSummaryGenerateCommand
import com.hirelog.api.job.application.summary.pipeline.SummaryGenerationPipeline
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class KafkaSummaryGenerationFacade(
    private val pipeline: SummaryGenerationPipeline
) {
    fun process(message: JobSummaryGenerateCommand): CompletableFuture<Void> {
        return pipeline.execute(message)
    }
}
