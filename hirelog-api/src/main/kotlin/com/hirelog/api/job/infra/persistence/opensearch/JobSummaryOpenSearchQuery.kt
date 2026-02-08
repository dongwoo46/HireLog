package com.hirelog.api.job.infra.persistence.opensearch

import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.query.JobSummarySearchItem
import com.hirelog.api.job.application.summary.query.JobSummarySearchQuery
import com.hirelog.api.job.application.summary.query.JobSummarySearchQuery.SortBy
import com.hirelog.api.job.application.summary.query.JobSummarySearchResult
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryIndexConstants.Fields
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryIndexConstants.INDEX_NAME
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.SortOptions
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery
import org.opensearch.client.opensearch._types.query_dsl.MultiMatchQuery
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch._types.query_dsl.TermQuery
import org.opensearch.client.opensearch._types.query_dsl.TermsQuery
import org.opensearch.client.opensearch._types.query_dsl.TextQueryType
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch.core.SearchRequest
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * JobSummary OpenSearch 검색 쿼리
 *
 * 책임:
 * - OpenSearch 검색 쿼리 실행
 * - 검색 결과 매핑
 *
 * 검색 전략:
 * - multi_match: 여러 필드에서 키워드 검색
 * - cross_fields: 한글/영어 multi-field 지원
 * - filter: 필터 조건 (careerType, brandId 등)
 */
@Component
class JobSummaryOpenSearchQuery(
    private val openSearchClient: OpenSearchClient
) {

    companion object {
        // 검색 대상 필드 (한글/영어 multi-field 포함)
        private val SEARCH_FIELDS = listOf(
            Fields.POSITION_NAME,
            "${Fields.POSITION_NAME}.${Fields.ENGLISH_SUFFIX}",
            Fields.BRAND_NAME,
            "${Fields.BRAND_NAME}.${Fields.ENGLISH_SUFFIX}",
            Fields.COMPANY_NAME,
            "${Fields.COMPANY_NAME}.${Fields.ENGLISH_SUFFIX}",
            Fields.BRAND_POSITION_NAME,
            "${Fields.BRAND_POSITION_NAME}.${Fields.ENGLISH_SUFFIX}",
            Fields.POSITION_CATEGORY_NAME,
            "${Fields.POSITION_CATEGORY_NAME}.${Fields.ENGLISH_SUFFIX}",
            Fields.SUMMARY_TEXT,
            "${Fields.SUMMARY_TEXT}.${Fields.ENGLISH_SUFFIX}",
            Fields.RESPONSIBILITIES,
            Fields.REQUIRED_QUALIFICATIONS,
            Fields.PREFERRED_QUALIFICATIONS,
            Fields.TECH_STACK,
            "${Fields.TECH_STACK}.${Fields.ENGLISH_SUFFIX}",
            Fields.IDEAL_CANDIDATE,
            Fields.KEY_CHALLENGES,
            Fields.TECHNICAL_CONTEXT
        )
    }

    /**
     * 검색 실행
     */
    fun search(query: JobSummarySearchQuery): JobSummarySearchResult {
        val searchRequest = buildSearchRequest(query)

        log.debug("[OPENSEARCH_SEARCH] query={}", query)

        val response = openSearchClient.search(searchRequest, Map::class.java)

        val items = response.hits().hits().mapNotNull { hit ->
            @Suppress("UNCHECKED_CAST")
            mapToSearchItem(hit.source() as? Map<String, Any?>)
        }

        val totalCount = response.hits().total()?.value() ?: 0L

        log.info(
            "[OPENSEARCH_SEARCH_RESULT] totalCount={}, returned={}",
            totalCount,
            items.size
        )

        return JobSummarySearchResult.of(
            items = items,
            totalCount = totalCount,
            page = query.page,
            size = query.size
        )
    }

    private fun buildSearchRequest(query: JobSummarySearchQuery): SearchRequest {
        return SearchRequest.Builder()
            .index(INDEX_NAME)
            .query(buildQuery(query))
            .from(query.page * query.size)
            .size(query.size)
            .sort(buildSort(query))
            .build()
    }

    private fun buildQuery(query: JobSummarySearchQuery): Query {
        val boolQuery = BoolQuery.Builder()

        // 키워드 검색
        if (!query.keyword.isNullOrBlank()) {
            boolQuery.must(buildKeywordQuery(query.keyword))
        }

        // 필터 조건들
        val filters = mutableListOf<Query>()

        query.careerType?.let { careerType ->
            filters.add(Query.of { q ->
                q.term(TermQuery.Builder()
                    .field(Fields.CAREER_TYPE)
                    .value(FieldValue.of(careerType.name))
                    .build())
            })
        }

        query.brandId?.let { brandId ->
            filters.add(Query.of { q ->
                q.term(TermQuery.Builder()
                    .field(Fields.BRAND_ID)
                    .value(FieldValue.of(brandId))
                    .build())
            })
        }

        query.companyId?.let { companyId ->
            filters.add(Query.of { q ->
                q.term(TermQuery.Builder()
                    .field(Fields.COMPANY_ID)
                    .value(FieldValue.of(companyId))
                    .build())
            })
        }

        query.positionId?.let { positionId ->
            filters.add(Query.of { q ->
                q.term(TermQuery.Builder()
                    .field(Fields.POSITION_ID)
                    .value(FieldValue.of(positionId))
                    .build())
            })
        }

        query.brandPositionId?.let { brandPositionId ->
            filters.add(Query.of { q ->
                q.term(TermQuery.Builder()
                    .field(Fields.BRAND_POSITION_ID)
                    .value(FieldValue.of(brandPositionId))
                    .build())
            })
        }

        query.positionCategoryId?.let { positionCategoryId ->
            filters.add(Query.of { q ->
                q.term(TermQuery.Builder()
                    .field(Fields.POSITION_CATEGORY_ID)
                    .value(FieldValue.of(positionCategoryId))
                    .build())
            })
        }

        // 기술스택 필터 (OR 조건)
        if (!query.techStacks.isNullOrEmpty()) {
            filters.add(Query.of { q ->
                q.terms(TermsQuery.Builder()
                    .field(Fields.TECH_STACK_PARSED)
                    .terms { t ->
                        t.value(query.techStacks.map { FieldValue.of(it) })
                    }
                    .build())
            })
        }

        if (filters.isNotEmpty()) {
            boolQuery.filter(filters)
        }

        // 키워드가 없고 필터도 없으면 match_all
        return if (query.keyword.isNullOrBlank() && filters.isEmpty()) {
            Query.of { q -> q.matchAll { it } }
        } else {
            Query.of { q -> q.bool(boolQuery.build()) }
        }
    }

    private fun buildKeywordQuery(keyword: String): Query {
        return Query.of { q ->
            q.multiMatch(MultiMatchQuery.Builder()
                .query(keyword)
                .fields(SEARCH_FIELDS)
                .type(TextQueryType.CrossFields)
                .build())
        }
    }

    private fun buildSort(query: JobSummarySearchQuery): List<SortOptions> {
        return when (query.sortBy) {
            SortBy.CREATED_AT_DESC -> listOf(
                SortOptions.of { s -> s.field { f -> f.field(Fields.CREATED_AT).order(SortOrder.Desc) } }
            )
            SortBy.CREATED_AT_ASC -> listOf(
                SortOptions.of { s -> s.field { f -> f.field(Fields.CREATED_AT).order(SortOrder.Asc) } }
            )
            SortBy.RELEVANCE -> listOf(
                SortOptions.of { s -> s.score { sc -> sc.order(SortOrder.Desc) } },
                SortOptions.of { s -> s.field { f -> f.field(Fields.CREATED_AT).order(SortOrder.Desc) } }
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapToSearchItem(source: Map<String, Any?>?): JobSummarySearchItem? {
        if (source == null) return null

        return try {
            JobSummarySearchItem(
                id = (source[Fields.ID] as? Number)?.toLong() ?: return null,
                brandId = (source[Fields.BRAND_ID] as? Number)?.toLong() ?: 0L,
                brandName = source[Fields.BRAND_NAME] as? String ?: "",
                companyName = source[Fields.COMPANY_NAME] as? String,
                positionId = (source[Fields.POSITION_ID] as? Number)?.toLong() ?: 0L,
                positionName = source[Fields.POSITION_NAME] as? String ?: "",
                brandPositionId = (source[Fields.BRAND_POSITION_ID] as? Number)?.toLong(),
                brandPositionName = source[Fields.BRAND_POSITION_NAME] as? String,
                positionCategoryId = (source[Fields.POSITION_CATEGORY_ID] as? Number)?.toLong() ?: 0L,
                positionCategoryName = source[Fields.POSITION_CATEGORY_NAME] as? String ?: "",
                careerType = source[Fields.CAREER_TYPE] as? String ?: "UNKNOWN",
                careerYears = source[Fields.CAREER_YEARS] as? String,
                summaryText = source[Fields.SUMMARY_TEXT] as? String ?: "",
                techStack = source[Fields.TECH_STACK] as? String,
                techStackParsed = source[Fields.TECH_STACK_PARSED] as? List<String>,
                createdAt = parseDateTime(source[Fields.CREATED_AT])
            )
        } catch (e: Exception) {
            log.warn("[OPENSEARCH_MAPPING_ERROR] Failed to map source: {}", e.message)
            null
        }
    }

    private fun parseDateTime(value: Any?): LocalDateTime {
        return when (value) {
            is String -> LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            is Number -> LocalDateTime.ofEpochSecond(value.toLong() / 1000, 0, java.time.ZoneOffset.UTC)
            else -> LocalDateTime.now()
        }
    }
}
