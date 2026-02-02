package com.hirelog.api.job.infra.persistence.opensearch

import com.hirelog.api.common.logging.log
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * JobSummary OpenSearch 인덱스 초기화
 *
 * 실행 시점:
 * - 애플리케이션 시작 완료 후
 * - 모든 빈 초기화 이후
 *
 * 멱등성:
 * - 인덱스가 이미 존재하면 생성하지 않음
 * - 다중 인스턴스 동시 시작 시에도 안전
 */
@Component
@Order(1)
class JobSummaryIndexInitializer(
    private val indexManager: JobSummaryIndexManager
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        try {
            log.info("[OPENSEARCH_INDEX_INIT] Starting index initialization...")
            indexManager.createIndexIfNotExists()
            log.info("[OPENSEARCH_INDEX_INIT] Index initialization completed")
        } catch (e: Exception) {
            // 인덱스 생성 실패해도 애플리케이션은 시작되어야 함
            // OpenSearch 연결 불가 시에도 다른 기능은 동작해야 함
            log.error("[OPENSEARCH_INDEX_INIT] Failed to initialize index. OpenSearch may be unavailable.", e)
        }
    }
}
