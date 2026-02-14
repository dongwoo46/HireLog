package com.hirelog.api.job.infra.external.fakellm

import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

class FakeGeminiJobSummaryLlm(
    private val executor: Executor,
    private val factory: FakeLlmResultFactory,
    private val circuitBreaker: CircuitBreaker,
    private val latencyMs: Long,
    private val failureRate: Int
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

                maybeFail()

                val seq = counter.incrementAndGet()

                factory.generate(
                    seq = seq,
                    provider = LlmProvider.GEMINI
                )
            }

        }, executor)
    }

    private fun simulateLatency() {
        try {
            Thread.sleep((latencyMs - 200..latencyMs + 200).random())
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun maybeFail() {
        if (Random.nextInt(100) < failureRate) {
            throw RuntimeException("Fake Gemini failure for load test")
        }
    }
}
