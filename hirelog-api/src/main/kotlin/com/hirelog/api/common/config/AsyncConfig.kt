package com.hirelog.api.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor

@Configuration
@EnableAsync
class AsyncConfig {

    @Bean(name = ["blockingExecutor"])
    fun blockingExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        // 스레드 풀 크기
        executor.corePoolSize = 10
        executor.maxPoolSize = 20
        executor.queueCapacity = 100

        // 스레드 이름 (로그에서 식별 용이)
        executor.setThreadNamePrefix("blocking-tx-")

        // 거부 정책: 풀이 가득 차면 호출한 스레드에서 실행
        executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())

        // Graceful Shutdown 설정
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(30)

        executor.initialize()
        return executor
    }
}