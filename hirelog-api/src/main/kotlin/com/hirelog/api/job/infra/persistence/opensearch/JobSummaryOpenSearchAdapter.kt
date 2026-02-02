package com.hirelog.api.job.infra.persistence.opensearch

import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.payload.JobSummarySearchPayload
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryIndexConstants.INDEX_NAME
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.core.IndexRequest
import org.springframework.stereotype.Component

/**
 * JobSummary OpenSearch Adapter
 *
 * 책임:
 * - JobSummary 문서 인덱싱
 * - OpenSearchClient 래핑
 *
 * 설계:
 * - 문서 ID = JobSummary.id (upsert 동작)
 * - 실패 시 예외 전파 (Consumer에서 재시도 처리)
 */
@Component
class JobSummaryOpenSearchAdapter(
    private val openSearchClient: OpenSearchClient
) {

    /**
     * JobSummary 문서 인덱싱
     *
     * 동작:
     * - 문서가 없으면 생성
     * - 문서가 있으면 덮어쓰기 (upsert)
     *
     * @param payload 인덱싱할 문서 데이터
     */
    fun index(payload: JobSummarySearchPayload) {
        val request = IndexRequest.Builder<JobSummarySearchPayload>()
            .index(INDEX_NAME)
            .id(payload.id.toString())
            .document(payload)
            .build()

        val response = openSearchClient.index(request)

        log.info(
            "[OPENSEARCH_INDEX] index={}, id={}, result={}",
            INDEX_NAME,
            payload.id,
            response.result().name
        )
    }

    /**
     * 벌크 인덱싱 (향후 배치 처리용)
     */
    fun bulkIndex(payloads: List<JobSummarySearchPayload>) {
        if (payloads.isEmpty()) return

        val bulkRequest = org.opensearch.client.opensearch.core.BulkRequest.Builder()

        payloads.forEach { payload ->
            bulkRequest.operations { op ->
                op.index { idx ->
                    idx.index(INDEX_NAME)
                        .id(payload.id.toString())
                        .document(payload)
                }
            }
        }

        val response = openSearchClient.bulk(bulkRequest.build())

        if (response.errors()) {
            val failedIds = response.items()
                .filter { it.error() != null }
                .map { it.id() }
            log.error("[OPENSEARCH_BULK_INDEX_PARTIAL_FAILURE] failedIds={}", failedIds)
        } else {
            log.info("[OPENSEARCH_BULK_INDEX] indexed={} documents", payloads.size)
        }
    }
}
