package com.hirelog.api.common.async

import org.slf4j.MDC
import java.util.concurrent.Executor

/**
 * MDC 전파 Executor 래퍼
 *
 * 책임:
 * - submit/execute 시점의 MDC 스냅샷을 캡처
 * - 실제 실행 스레드에서 MDC를 복원 후 작업 수행
 * - 작업 종료 후 이전 MDC 상태 복구
 *
 * 사용처:
 * - CompletableFuture.thenAcceptAsync(..., mdcExecutor)
 * - CompletableFuture.exceptionallyAsync(..., mdcExecutor)
 */
class MdcExecutor(private val delegate: Executor) : Executor {

    override fun execute(command: Runnable) {
        val callerMdc = MDC.getCopyOfContextMap() ?: emptyMap()

        delegate.execute {
            val previousMdc = MDC.getCopyOfContextMap()
            MDC.setContextMap(callerMdc)
            try {
                command.run()
            } finally {
                if (previousMdc != null) MDC.setContextMap(previousMdc) else MDC.clear()
            }
        }
    }
}
