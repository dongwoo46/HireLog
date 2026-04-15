package com.hirelog.api.job.infra.persistence.opensearch

import com.hirelog.api.common.exception.InvalidCursorException
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.query.JobSummarySearchItem
import com.hirelog.api.job.application.summary.query.JobSummarySearchQuery
import com.hirelog.api.job.application.summary.query.JobSummarySearchQuery.SortBy
import com.hirelog.api.job.application.summary.query.JobSummarySearchResult
import com.hirelog.api.job.application.summary.query.SearchCursor
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryIndexConstants.Fields
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryIndexConstants.INDEX_NAME
import com.hirelog.api.relation.application.memberjobsummary.port.MemberJobSummaryQuery
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.OpenSearchException
import org.opensearch.client.opensearch._types.SortOptions
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery
import org.opensearch.client.opensearch._types.query_dsl.MatchQuery
import org.opensearch.client.opensearch._types.query_dsl.MultiMatchQuery
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch._types.query_dsl.TermsQuery
import org.opensearch.client.opensearch._types.query_dsl.TextQueryType
import org.opensearch.client.opensearch.core.SearchRequest
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * JobSummary OpenSearch 검색 쿼리 (Search After 무한스크롤)
 *
 * 페이징 전략:
 * - offset 방식(from) 제거 → search_after 커서 방식
 * - size+1 fetch로 hasNext 판별
 * - 커서: 마지막 히트의 sort 값을 Base64 JSON으로 인코딩
 *
 * 커서 검증:
 * - 잘못된 Base64/JSON → InvalidCursorException (400)
 * - sortBy와 cursor 타입 불일치 → InvalidCursorException (400)
 *
 * 정렬:
 * - 모든 sort에 id DESC tiebreaker 추가 (search_after 중복 방지)
 */
@Component
class JobSummaryOpenSearchQuery(
    private val openSearchClient: OpenSearchClient,
    private val memberJobSummaryQuery: MemberJobSummaryQuery
) {

    companion object {
        private const val POPULAR_CANDIDATE_LIMIT = 800
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

    fun search(query: JobSummarySearchQuery): JobSummarySearchResult {
        // cursor 존재 시 decode (실패 → InvalidCursorException)
        // sortBy 불일치 시 InvalidCursorException
        val cursor = query.cursor?.let { encoded ->
            SearchCursor.decode(encoded).also { validateCursorCompatibility(it, query.sortBy) }
        }

        if (query.sortBy == SortBy.SAVE_COUNT_DESC) {
            return searchBySaveCount(query, cursor as? SearchCursor.Popular)
        }

        val searchRequest = buildSearchRequest(query, cursor)

        val response = try {
            openSearchClient.search(searchRequest, Map::class.java)
        } catch (e: OpenSearchException) {
            if (e.error().type() == "index_not_found_exception") {
                log.warn("[OPENSEARCH_INDEX_NOT_FOUND] index={}", INDEX_NAME)
                return JobSummarySearchResult(items = emptyList(), size = 0, hasNext = false, nextCursor = null)
            }
            throw e
        }
        val hits = response.hits().hits()

        val hasNext = hits.size > query.size
        val pageHits = if (hasNext) hits.dropLast(1) else hits

        val items = pageHits.mapNotNull { hit ->
            @Suppress("UNCHECKED_CAST")
            mapToSearchItem(hit.source() as? Map<String, Any?>)
        }

        val nextCursor = if (hasNext && pageHits.isNotEmpty()) {
            generateNextCursor(pageHits.last().sort(), query.sortBy)
        } else null

        return JobSummarySearchResult(
            items = items,
            size = items.size,
            hasNext = hasNext,
            nextCursor = nextCursor
        )
    }

    /**
     * cursor 타입과 sortBy 일치 여부 검증
     *
     * - CreatedAt cursor + RELEVANCE sortBy → 불일치
     * - Relevance cursor + CREATED_AT_* sortBy → 불일치
     *
     * 불일치 시 InvalidCursorException (400)
     */
    private fun validateCursorCompatibility(cursor: SearchCursor, sortBy: SortBy) {
        when {
            cursor is SearchCursor.Popular && sortBy != SortBy.SAVE_COUNT_DESC ->
                throw InvalidCursorException(
                    "Cursor type 'popular' is incompatible with sortBy=$sortBy. Expected sortBy=SAVE_COUNT_DESC"
                )
            cursor is SearchCursor.Relevance && sortBy != SortBy.RELEVANCE ->
                throw InvalidCursorException(
                    "Cursor type 'relevance' is incompatible with sortBy=$sortBy. Expected sortBy=RELEVANCE"
                )
            cursor is SearchCursor.CreatedAt && sortBy !in setOf(SortBy.CREATED_AT_DESC, SortBy.CREATED_AT_ASC) ->
                throw InvalidCursorException(
                    "Cursor type 'createdAt' is incompatible with sortBy=$sortBy. Expected sortBy=CREATED_AT_DESC or CREATED_AT_ASC"
                )
        }
    }

    private fun buildSearchRequest(query: JobSummarySearchQuery, cursor: SearchCursor?): SearchRequest {
        val builder = SearchRequest.Builder()
            .index(INDEX_NAME)
            .query(buildQuery(query))
            .size(query.size + 1)
            .sort(buildSort(query))

        if (cursor != null) {
            builder.searchAfter(buildSearchAfterValues(cursor))
        }

        return builder.build()
    }

    /**
     * search_after 값 목록 생성
     *
     * OpenSearch Java 클라이언트 API: searchAfter(List<String>)
     * - 숫자 필드: 문자열 표현 그대로 전달 (OpenSearch가 타입 강제)
     * - date 필드: epoch millis 문자열 → OpenSearch가 Long으로 해석
     */
    private fun buildSearchAfterValues(cursor: SearchCursor): List<String> {
        return when (cursor) {
            is SearchCursor.CreatedAt -> listOf(
                cursor.createdAtMillis.toString(),
                cursor.id.toString()
            )
            is SearchCursor.Relevance -> listOf(
                cursor.score.toString(),
                cursor.createdAtMillis.toString(),
                cursor.id.toString()
            )
            is SearchCursor.Popular -> emptyList()
        }
    }

    private fun buildQuery(query: JobSummarySearchQuery): Query {
        val boolQuery = BoolQuery.Builder()

        if (!query.keyword.isNullOrBlank()) {
            boolQuery.should(buildKeywordQuery(query.keyword))
            boolQuery.minimumShouldMatch("1")
        }

        val filterQueries = mutableListOf<Query>()

        query.careerTypes
            ?.map { it.name }
            ?.takeIf { it.isNotEmpty() }
            ?.let { filterQueries += buildStringTermsQuery(Fields.CAREER_TYPE, it) }

        query.brandIds
            ?.takeIf { it.isNotEmpty() }
            ?.let { filterQueries += buildLongTermsQuery(Fields.BRAND_ID, it) }

        query.companyIds
            ?.takeIf { it.isNotEmpty() }
            ?.let { filterQueries += buildLongTermsQuery(Fields.COMPANY_ID, it) }

        query.positionIds
            ?.takeIf { it.isNotEmpty() }
            ?.let { filterQueries += buildLongTermsQuery(Fields.POSITION_ID, it) }

        query.brandPositionIds
            ?.takeIf { it.isNotEmpty() }
            ?.let { filterQueries += buildLongTermsQuery(Fields.BRAND_POSITION_ID, it) }

        query.positionCategoryIds
            ?.takeIf { it.isNotEmpty() }
            ?.let { filterQueries += buildLongTermsQuery(Fields.POSITION_CATEGORY_ID, it) }

        query.brandNames
            ?.takeIf { it.isNotEmpty() }
            ?.let { filterQueries += buildOrMatchQuery(Fields.BRAND_NAME, it) }

        query.positionNames
            ?.takeIf { it.isNotEmpty() }
            ?.let { filterQueries += buildOrMatchQuery(Fields.POSITION_NAME, it) }

        query.brandPositionNames
            ?.takeIf { it.isNotEmpty() }
            ?.let { filterQueries += buildOrMatchQuery(Fields.BRAND_POSITION_NAME, it) }

        query.positionCategoryNames
            ?.takeIf { it.isNotEmpty() }
            ?.let { filterQueries += buildOrMatchQuery(Fields.POSITION_CATEGORY_NAME, it) }

        query.techStacks
            ?.takeIf { it.isNotEmpty() }
            ?.let { filterQueries += buildStringTermsQuery(Fields.TECH_STACK_PARSED, it) }

        query.companyDomains
            ?.takeIf { it.isNotEmpty() }
            ?.let { filterQueries += buildStringTermsQuery(Fields.COMPANY_DOMAIN, it) }

        query.companySizes
            ?.takeIf { it.isNotEmpty() }
            ?.let { filterQueries += buildStringTermsQuery(Fields.COMPANY_SIZE, it) }

        if (filterQueries.isNotEmpty()) {
            filterQueries.forEach { boolQuery.filter(it) }
        }

        return if (query.keyword.isNullOrBlank() && filterQueries.isEmpty()) {
            Query.of { q -> q.matchAll { it } }
        } else {
            Query.of { q -> q.bool(boolQuery.build()) }
        }
    }

    private fun buildStringTermsQuery(field: String, values: List<String>): Query {
        return Query.of { q ->
            q.terms(TermsQuery.Builder()
                .field(field)
                .terms { t ->
                    t.value(values.map { FieldValue.of(it) })
                }
                .build())
        }
    }

    private fun buildLongTermsQuery(field: String, values: List<Long>): Query {
        return Query.of { q ->
            q.terms(TermsQuery.Builder()
                .field(field)
                .terms { t ->
                    t.value(values.map { FieldValue.of(it) })
                }
                .build())
        }
    }

    private fun buildOrMatchQuery(field: String, values: List<String>): Query {
        val builder = BoolQuery.Builder()
        values.forEach { value ->
            builder.should(
                Query.of { q ->
                    q.match(MatchQuery.Builder()
                        .field(field)
                        .query(FieldValue.of(value))
                        .build())
                }
            )
        }
        builder.minimumShouldMatch("1")
        return Query.of { q -> q.bool(builder.build()) }
    }

    private fun buildKeywordQuery(keyword: String): Query {
        return Query.of { q ->
            q.multiMatch(MultiMatchQuery.Builder()
                .query(keyword)
                .fields(SEARCH_FIELDS)
                .type(TextQueryType.BestFields)
                .build())
        }
    }

    /**
     * 정렬 옵션
     *
     * id DESC tiebreaker: 동일 createdAt/score에서 중복 없는 커서 보장
     */
    private fun buildSort(query: JobSummarySearchQuery): List<SortOptions> {
        val idTiebreaker = SortOptions.of { s ->
            s.field { f -> f.field(Fields.ID).order(SortOrder.Desc) }
        }

        return when (query.sortBy) {
            SortBy.SAVE_COUNT_DESC -> listOf(
                SortOptions.of { s -> s.field { f -> f.field(Fields.CREATED_AT).order(SortOrder.Desc) } },
                idTiebreaker
            )
            SortBy.CREATED_AT_DESC -> listOf(
                SortOptions.of { s -> s.field { f -> f.field(Fields.CREATED_AT).order(SortOrder.Desc) } },
                idTiebreaker
            )
            SortBy.CREATED_AT_ASC -> listOf(
                SortOptions.of { s -> s.field { f -> f.field(Fields.CREATED_AT).order(SortOrder.Asc) } },
                idTiebreaker
            )
            SortBy.RELEVANCE -> listOf(
                SortOptions.of { s -> s.score { sc -> sc.order(SortOrder.Desc) } },
                SortOptions.of { s -> s.field { f -> f.field(Fields.CREATED_AT).order(SortOrder.Desc) } },
                idTiebreaker
            )
        }
    }

    /**
     * 마지막 히트의 sort 값으로 nextCursor 생성
     *
     * OpenSearch Java 클라이언트 API: hit.sort() → List<String>
     * - 파싱 실패(NumberFormatException, IndexOutOfBoundsException)는 try/catch 없이 전파
     * - 예상 외 응답 → 500으로 fail-fast (OpenSearch 매핑 오류 조기 감지)
     */
    private fun generateNextCursor(sortValues: List<String>, sortBy: SortBy): String? {
        if (sortValues.isEmpty()) return null

        return when (sortBy) {
            SortBy.SAVE_COUNT_DESC -> null
            SortBy.CREATED_AT_DESC, SortBy.CREATED_AT_ASC -> {
                val createdAtMillis = sortValues[0].toLong()
                val id = sortValues[1].toLong()
                SearchCursor.encode(SearchCursor.CreatedAt(createdAtMillis, id))
            }
            SortBy.RELEVANCE -> {
                val score = sortValues[0].toDouble()
                val createdAtMillis = sortValues[1].toLong()
                val id = sortValues[2].toLong()
                SearchCursor.encode(SearchCursor.Relevance(score, createdAtMillis, id))
            }
        }
    }

    fun searchTechStacks(keyword: String?, size: Int): List<String> {
        val normalizedKeyword = keyword?.trim()?.takeIf { it.isNotEmpty() }
        val limitedSize = size.coerceIn(1, 100)

        val query = if (normalizedKeyword == null) {
            Query.of { q -> q.matchAll { it } }
        } else {
            Query.of { q ->
                q.bool { b ->
                    b.should {
                        it.match(
                            MatchQuery.Builder()
                                .field(Fields.TECH_STACK_PARSED)
                                .query(FieldValue.of(normalizedKeyword))
                                .build()
                        )
                    }
                    b.should {
                        it.match(
                            MatchQuery.Builder()
                                .field(Fields.TECH_STACK)
                                .query(FieldValue.of(normalizedKeyword))
                                .build()
                        )
                    }
                    b.minimumShouldMatch("1")
                }
            }
        }

        val request = SearchRequest.Builder()
            .index(INDEX_NAME)
            .query(query)
            .size(300)
            .sort(
                listOf(
                    SortOptions.of { s -> s.field { f -> f.field(Fields.CREATED_AT).order(SortOrder.Desc) } },
                    SortOptions.of { s -> s.field { f -> f.field(Fields.ID).order(SortOrder.Desc) } }
                )
            )
            .build()

        val response = try {
            openSearchClient.search(request, Map::class.java)
        } catch (e: OpenSearchException) {
            if (e.error().type() == "index_not_found_exception") {
                log.warn("[OPENSEARCH_INDEX_NOT_FOUND] index={}", INDEX_NAME)
                return emptyList()
            }
            throw e
        }
        val stacks = response.hits().hits()
            .mapNotNull { hit -> hit.source() as? Map<String, Any?> }
            .flatMap { source ->
                @Suppress("UNCHECKED_CAST")
                (source[Fields.TECH_STACK_PARSED] as? List<String>).orEmpty()
            }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { stack ->
                normalizedKeyword == null || stack.contains(normalizedKeyword, ignoreCase = true)
            }
            .distinct()

        return stacks.take(limitedSize)
    }

    private fun searchBySaveCount(
        query: JobSummarySearchQuery,
        cursor: SearchCursor.Popular?
    ): JobSummarySearchResult {
        val candidateRequest = SearchRequest.Builder()
            .index(INDEX_NAME)
            .query(buildQuery(query))
            .size(POPULAR_CANDIDATE_LIMIT)
            .sort(
                listOf(
                    SortOptions.of { s -> s.field { f -> f.field(Fields.CREATED_AT).order(SortOrder.Desc) } },
                    SortOptions.of { s -> s.field { f -> f.field(Fields.ID).order(SortOrder.Desc) } }
                )
            )
            .build()

        val response = try {
            openSearchClient.search(candidateRequest, Map::class.java)
        } catch (e: OpenSearchException) {
            if (e.error().type() == "index_not_found_exception") {
                log.warn("[OPENSEARCH_INDEX_NOT_FOUND] index={}", INDEX_NAME)
                return JobSummarySearchResult(items = emptyList(), size = 0, hasNext = false, nextCursor = null)
            }
            throw e
        }
        val candidates = response.hits().hits()
            .mapNotNull { hit ->
                @Suppress("UNCHECKED_CAST")
                mapToSearchItem(hit.source() as? Map<String, Any?>)
            }

        val saveCounts = memberJobSummaryQuery.countSavedByJobSummaryIds(candidates.map { it.id }.toSet())
        val sorted = candidates.sortedWith(
            compareByDescending<JobSummarySearchItem> { saveCounts[it.id] ?: 0L }
                .thenByDescending { it.createdAt }
                .thenByDescending { it.id }
        )

        val offset = cursor?.offset ?: 0
        if (offset >= sorted.size) {
            return JobSummarySearchResult(
                items = emptyList(),
                size = 0,
                hasNext = false,
                nextCursor = null
            )
        }

        val items = sorted.drop(offset).take(query.size)
        val nextOffset = offset + items.size
        val hasNext = nextOffset < sorted.size

        return JobSummarySearchResult(
            items = items,
            size = items.size,
            hasNext = hasNext,
            nextCursor = if (hasNext) SearchCursor.encode(SearchCursor.Popular(nextOffset)) else null
        )
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
                companyDomain = source[Fields.COMPANY_DOMAIN] as? String,
                companySize = source[Fields.COMPANY_SIZE] as? String,
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
