package com.hirelog.api.job.application.summary

import com.hirelog.api.job.application.summary.command.JobSummaryGenerateCommand
import com.hirelog.api.job.application.summary.pipeline.SummaryGenerationPipeline
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * RedisSummaryGenerationFacade
 *
 * 책임:
 * - Redis Stream 기반 요약 생성 트리거
 * - Executor를 통한 비동기 실행 제어
 *
 * 주의:
 * - Redis 메시지 타입에는 의존하지 않는다
 * - Application Command만 처리한다
 */
@Service
class RedisSummaryGenerationFacade(
    private val pipeline: SummaryGenerationPipeline
) {

    fun process(
        command: JobSummaryGenerateCommand,
        executor: Executor
    ): CompletableFuture<Void> {
        return CompletableFuture
            .runAsync(
                { pipeline.execute(command) },
                executor
            )
    }
}
