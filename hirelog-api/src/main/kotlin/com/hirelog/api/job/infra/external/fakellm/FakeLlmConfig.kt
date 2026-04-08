package com.hirelog.api.job.infra.external.fakellm

import com.hirelog.api.common.logging.log
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@ConditionalOnProperty(name = ["hirelog.loadtest.fake-llm"], havingValue = "true")
class FakeLlmConfig {

    @Bean
    fun fakeOpenAiCircuitBreaker(
        registry: CircuitBreakerRegistry
    ): CircuitBreaker {
        return registry.circuitBreaker("fake-openai")
    }

    @Bean
    fun fakeGeminiCircuitBreaker(
        registry: CircuitBreakerRegistry
    ): CircuitBreaker {
        return registry.circuitBreaker("fake-gemini")
    }

    @Bean("fakeLlmExecutor")
    fun fakeLlmExecutor(
        meterRegistry: MeterRegistry
    ): Executor {
        log.info("[FAKE_LLM_CONFIG] fakeLlmExecutor initialized")

        val executor = ThreadPoolTaskExecutor().apply {
            corePoolSize = 20
            maxPoolSize = 50
            queueCapacity = 500
            setThreadNamePrefix("fake-llm-")
            initialize() // 반드시 먼저 호출
        }

        // 실제 사용하는 ThreadPoolExecutor 인스턴스를 모니터링에 등록
        ExecutorServiceMetrics.monitor(
            meterRegistry,
            executor.threadPoolExecutor,
            "llm_executor"
        )
        return executor
    }

    @Bean("geminiJobSummaryLlm")
    fun fakeGemini(
        @Qualifier("fakeLlmExecutor")
        executor: Executor,
        positionPoolProvider: FakeLlmResultFactory,
        fakeGeminiCircuitBreaker: CircuitBreaker
    ): FakeGeminiJobSummaryLlm {

        log.info("[FAKE_LLM_CONFIG] FakeGeminiJobSummaryLlm bean created")

        return FakeGeminiJobSummaryLlm(
            executor = executor,
            factory = positionPoolProvider,
            circuitBreaker = fakeGeminiCircuitBreaker,
            latencyMs = 800,
            failureRate = 20
        )
    }


    @Bean("openAiJobSummaryLlm")
    fun fakeOpenAi(
        @Qualifier("fakeLlmExecutor")
        executor: Executor,
        positionPoolProvider: FakeLlmResultFactory,
        fakeOpenAiCircuitBreaker: CircuitBreaker   // 🔥 추가
    ): FakeOpenAiJobSummaryLlm {

        log.info("[FAKE_LLM_CONFIG] FakeOpenAiJobSummaryLlm bean created")

        return FakeOpenAiJobSummaryLlm(
            executor = executor,
            factory = positionPoolProvider,
            circuitBreaker = fakeOpenAiCircuitBreaker,
            latencyMs = 1000,
            failureRate = 20
        )
    }

}
