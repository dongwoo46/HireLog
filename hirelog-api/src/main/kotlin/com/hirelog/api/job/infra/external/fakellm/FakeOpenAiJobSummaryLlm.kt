package com.hirelog.api.job.infra.external.fakellm

import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong

class FakeOpenAiJobSummaryLlm(
    private val executor: Executor,
    private val factory: FakeLlmResultFactory,
    private val circuitBreaker: CircuitBreaker,
    private val latencyMs: Long,
    private val failureRate: Int // 0~100
) : JobSummaryLlm {

    private val counter = AtomicLong(0)

    override fun summarizeJobDescriptionAsync(
        brandName: String,
        positionName: String,
        positionCandidates: List<String>,
        existCompanies: List<String>,
        canonicalMap: Map<String, List<String>>
    ): CompletableFuture<JobSummaryLlmResult> {

        return CompletableFuture.supplyAsync({

            circuitBreaker.executeSupplier {

                simulateLatency()
                simulateFailure()

                val seq = counter.incrementAndGet()

                factory.generate(
                    seq = seq,
                    provider = LlmProvider.OPENAI
                )
            }

        }, executor)
    }

    private fun simulateLatency() {
        try {
            Thread.sleep(latencyMs)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun simulateFailure() {
        val random = ThreadLocalRandom.current().nextInt(100)
        if (random < failureRate) {
            throw RuntimeException("Fake OpenAI failure")
        }
    }
}
