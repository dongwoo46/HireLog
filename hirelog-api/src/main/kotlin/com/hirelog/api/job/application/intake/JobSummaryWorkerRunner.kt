package com.hirelog.api.job.application.intake

import com.hirelog.api.common.config.properties.HirelogWorkerProperties
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.intake.worker.JdSummaryGenerationWorker
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * JobSummaryWorkerRunner
 *
 * 책임:
 * - 애플리케이션 기동 시
 * - JD Summary Worker 실행 트리거
 *
 * 주의:
 * - run()은 딱 1번만 호출됨
 */
@Component
class JobSummaryWorkerRunner(
    private val worker: JdSummaryGenerationWorker,
    private val workerProperties: HirelogWorkerProperties
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {

        log.info(
            "[JOB_SUMMARY_WORKER_RUNNER] enabled={}",
            workerProperties.jobSummary.enabled
        )

        if (!workerProperties.jobSummary.enabled) {
            log.info("[JOB_SUMMARY_WORKER_RUNNER] worker disabled, skip start")
            return
        }

        log.info("[JOB_SUMMARY_WORKER_RUNNER] starting worker")
        worker.startConsuming()
    }
}
