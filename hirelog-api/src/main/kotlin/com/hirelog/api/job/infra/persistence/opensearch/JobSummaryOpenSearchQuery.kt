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
import org.opensearch.client.opensearch._types.query_dsl.MatchQuery
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
 * 검색 전략:
 * - keyword: multi_match + best_fields + should + minimum_should_match
 * - ID 필터: term (filter context, AND)
 * - Name 필터: match (filter context, AND)
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

        // 키워드 검색 (should + minimum_should_match)
        if (!query.keyword.isNullOrBlank()) {
            boolQuery.should(buildKeywordQuery(query.keyword))
            boolQuery.minimumShouldMatch("1")
        }

        // 필터 조건들 (filter context, AND)
        val filters = mutableListOf<Query>()

        // === ID 필터 (term) ===
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

        // === Name 필터 (match) ===
        query.brandName?.let { brandName ->
            filters.add(Query.of { q ->
                q.match(MatchQuery.Builder()
                    .field(Fields.BRAND_NAME)
                    .query(FieldValue.of(brandName))
                    .build())
            })
        }

        query.positionName?.let { positionName ->
            filters.add(Query.of { q ->
                q.match(MatchQuery.Builder()
                    .field(Fields.POSITION_NAME)
                    .query(FieldValue.of(positionName))
                    .build())
            })
        }

        query.brandPositionName?.let { brandPositionName ->
            filters.add(Query.of { q ->
                q.match(MatchQuery.Builder()
                    .field(Fields.BRAND_POSITION_NAME)
                    .query(FieldValue.of(brandPositionName))
                    .build())
            })
        }

        query.positionCategoryName?.let { positionCategoryName ->
            filters.add(Query.of { q ->
                q.match(MatchQuery.Builder()
                    .field(Fields.POSITION_CATEGORY_NAME)
                    .query(FieldValue.of(positionCategoryName))
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

    /**
     * 키워드 검색 쿼리
     *
     * best_fields: 각 필드 독립적으로 매칭 → best score 사용
     * - nori 필드에서 한글 매칭
     * - english 필드에서 영어 매칭
     * - analyzer 불일치 문제 없음
     */
    private fun buildKeywordQuery(keyword: String): Query {
        return Query.of { q ->
            q.multiMatch(MultiMatchQuery.Builder()
                .query(keyword)
                .fields(SEARCH_FIELDS)
                .type(TextQueryType.BestFields)
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
                brandName = source[Fields.BRAND_NAME] as? String ?: "",
                brandPositionName = source[Fields.BRAND_POSITION_NAME] as? String,
                positionCategoryName = source[Fields.POSITION_CATEGORY_NAME] as? String ?: "",
                careerType = source[Fields.CAREER_TYPE] as? String ?: "UNKNOWN",
                summaryText = source[Fields.SUMMARY_TEXT] as? String ?: "",
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
