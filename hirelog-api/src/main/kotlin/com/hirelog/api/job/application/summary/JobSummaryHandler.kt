package com.hirelog.api.job.application.summary

import com.hirelog.api.job.application.summary.command.JobSummaryGenerateCommand
import com.hirelog.api.job.application.summary.pipeline.JdSummaryGenerationFacade
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class JobSummaryHandler(
    private val facade: JdSummaryGenerationFacade
) {
    fun process(message: JobSummaryGenerateCommand): CompletableFuture<Void> {
        return facade.execute(message)
    }
}
