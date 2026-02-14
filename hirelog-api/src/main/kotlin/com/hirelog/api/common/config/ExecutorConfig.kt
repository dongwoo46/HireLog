package com.hirelog.api.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class ExecutorConfig {

    @Bean(name = ["hirelogTaskExecutor"])
    fun hirelogTaskExecutor(): ThreadPoolTaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 8
        executor.maxPoolSize = 16
        executor.queueCapacity = 200
        executor.setThreadNamePrefix("hirelog-exec-")
        executor.initialize()
        return executor
    }
}
