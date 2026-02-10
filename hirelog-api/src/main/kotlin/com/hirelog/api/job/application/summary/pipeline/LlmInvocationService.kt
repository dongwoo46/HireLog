package com.hirelog.api.job.application.summary.pipeline

import com.hirelog.api.job.application.summary.command.JobSummaryGenerateCommand
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class LlmInvocationService(
    @Qualifier("geminiJobSummaryLlm") private val primary: JobSummaryLlm,
    @Qualifier("openAiJobSummaryLlm") private val fallback: JobSummaryLlm
) {

    fun invoke(
        command: JobSummaryGenerateCommand,
        positionCandidates: List<String>,
        existCompanies: List<String>
    ): CompletableFuture<JobSummaryLlmResult> =
        primary.summarizeJobDescriptionAsync(
            command.brandName,
            command.positionName,
            positionCandidates,
            existCompanies,
            command.canonicalMap
        ).exceptionallyCompose {
            fallback.summarizeJobDescriptionAsync(
                command.brandName,
                command.positionName,
                positionCandidates,
                existCompanies,
                command.canonicalMap
            )
        }
}
