package com.hirelog.api.common.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class ExecutorMetricsConfig(
    private val meterRegistry: MeterRegistry,
    @Qualifier("hirelogTaskExecutor")
    private val hirelogExecutor: ThreadPoolTaskExecutor
) {

    @PostConstruct
    fun bind() {
        ExecutorServiceMetrics.monitor(
            meterRegistry,
            hirelogExecutor.threadPoolExecutor,
            "executor",
            Tags.of("name", "hirelogTaskExecutor")
        )
    }
}



