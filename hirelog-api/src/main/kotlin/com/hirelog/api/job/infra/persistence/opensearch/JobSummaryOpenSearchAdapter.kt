package com.hirelog.api.job.infra.persistence.opensearch

import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.payload.JobSummarySearchPayload
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryIndexConstants.Fields
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryIndexConstants.INDEX_NAME
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.aggregations.Aggregation
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch.core.IndexRequest
import org.opensearch.client.opensearch.core.SearchRequest
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
        try {
            val request = IndexRequest.Builder<JobSummarySearchPayload>()
                .index(INDEX_NAME)
                .id(payload.id.toString())
                .document(payload)
                .build()

            val response = openSearchClient.index(request)

            log.info(
                "[OPENSEARCH_INDEX_SUCCESS] index={}, id={}, result={}, version={}",
                INDEX_NAME,
                payload.id,
                response.result().name,
                response.version()
            )
        } catch (e: Exception) {
            log.error(
                "[OPENSEARCH_INDEX_FAILED] index={}, id={}, brandName={}, positionName={}, errorClass={}, errorMessage={}",
                INDEX_NAME,
                payload.id,
                payload.brandName,
                payload.positionName,
                e.javaClass.simpleName,
                e.message,
                e
            )
            throw e
        }
    }

    /**
     * JobSummary 문서 삭제
     *
     * 동작:
     * - 문서가 있으면 삭제
     * - 문서가 없으면 무시 (not_found)
     *
     * @param id 삭제할 문서 ID (JobSummary.id)
     */
    fun delete(id: Long) {
        try {
            val request = org.opensearch.client.opensearch.core.DeleteRequest.Builder()
                .index(INDEX_NAME)
                .id(id.toString())
                .build()

            val response = openSearchClient.delete(request)

            log.info(
                "[OPENSEARCH_DELETE_SUCCESS] index={}, id={}, result={}",
                INDEX_NAME,
                id,
                response.result().name
            )
        } catch (e: Exception) {
            log.error(
                "[OPENSEARCH_DELETE_FAILED] index={}, id={}, errorClass={}, errorMessage={}",
                INDEX_NAME,
                id,
                e.javaClass.simpleName,
                e.message,
                e
            )
            throw e
        }
    }

    /**
     * 임베딩 벡터 누락 문서 조회
     *
     * embeddingVector 필드가 없는 문서를 조회하여 재임베딩 대상 반환
     */
    fun findMissingEmbedding(size: Int): List<EmbeddingCandidate> {
        require(size in 1..500) { "size must be between 1 and 500" }

        val request = SearchRequest.Builder()
            .index(INDEX_NAME)
            .query { q ->
                q.bool { b ->
                    b.mustNot { mn ->
                        mn.exists { e -> e.field(Fields.EMBEDDING_VECTOR) }
                    }
                }
            }
            .source { s ->
                s.filter { f ->
                    f.includes(
                        listOf(
                            Fields.ID,
                            Fields.RESPONSIBILITIES,
                            Fields.REQUIRED_QUALIFICATIONS,
                            Fields.PREFERRED_QUALIFICATIONS,
                            Fields.IDEAL_CANDIDATE,
                            Fields.MUST_HAVE_SIGNALS,
                            Fields.TECHNICAL_CONTEXT
                        )
                    )
                }
            }
            .size(size)
            .build()

        val response = openSearchClient.search(request, Map::class.java)

        return response.hits().hits().mapNotNull { hit ->
            val src = hit.source() ?: return@mapNotNull null
            @Suppress("UNCHECKED_CAST")
            val map = src as Map<String, Any?>
            val id = (map[Fields.ID] as? Number)?.toLong() ?: return@mapNotNull null
            EmbeddingCandidate(
                id = id,
                responsibilities = map[Fields.RESPONSIBILITIES] as? String ?: return@mapNotNull null,
                requiredQualifications = map[Fields.REQUIRED_QUALIFICATIONS] as? String ?: return@mapNotNull null,
                preferredQualifications = map[Fields.PREFERRED_QUALIFICATIONS] as? String,
                idealCandidate = map[Fields.IDEAL_CANDIDATE] as? String,
                mustHaveSignals = map[Fields.MUST_HAVE_SIGNALS] as? String,
                technicalContext = map[Fields.TECHNICAL_CONTEXT] as? String
            )
        }
    }

    /**
     * 임베딩 벡터만 부분 업데이트
     */
    fun updateEmbeddingVector(id: Long, vector: List<Float>) {
        openSearchClient.update({ req ->
            req.index(INDEX_NAME)
                .id(id.toString())
                .doc(mapOf(Fields.EMBEDDING_VECTOR to vector))
        }, Map::class.java)

        log.info("[OPENSEARCH_EMBEDDING_UPDATE] id={}", id)
    }

    data class EmbeddingCandidate(
        val id: Long,
        val responsibilities: String,
        val requiredQualifications: String,
        val preferredQualifications: String?,
        val idealCandidate: String?,
        val mustHaveSignals: String?,
        val technicalContext: String?
    )

    /**
     * k-NN 벡터 검색 (RAG 전용)
     *
     * 처리 흐름:
     * 1. 쿼리 벡터 → k-NN 검색 (hnsw, cosinesimil)
     * 2. 선택적 필터 (careerType, companyDomain 등) 적용
     * 3. 반환 필드: id, score, brandName, positionName + JD/Insight 핵심 필드
     *
     * @param queryVector  쿼리 임베딩 벡터 (768차원)
     * @param k            반환할 문서 수
     * @param careerType   필터 (null이면 전체)
     * @param companyDomain 필터 (null이면 전체)
     */
    fun searchByVector(
        queryVector: List<Float>,
        k: Int,
        careerType: String? = null,
        companyDomain: String? = null
    ): List<KnnSearchResult> {
        val filterClauses = buildList<Query> {
            careerType?.let {
                add(Query.of { q -> q.term { t -> t.field(Fields.CAREER_TYPE).value { v -> v.stringValue(it) } } })
            }
            companyDomain?.let {
                add(Query.of { q -> q.term { t -> t.field(Fields.COMPANY_DOMAIN).value { v -> v.stringValue(it) } } })
            }
        }

        val request = SearchRequest.Builder()
            .index(INDEX_NAME)
            .query { q ->
                q.knn { knn ->
                    knn.field(Fields.EMBEDDING_VECTOR)
                        .vector(queryVector.toFloatArray())
                        .k(k)
                        .apply {
                            if (filterClauses.isNotEmpty()) {
                                filter(Query.of { fq -> fq.bool { b -> b.must(filterClauses) } })
                            }
                        }
                }
            }
            .source { s ->
                s.filter { f ->
                    f.includes(
                        listOf(
                            Fields.ID, Fields.BRAND_NAME, Fields.POSITION_NAME,
                            Fields.COMPANY_DOMAIN, Fields.COMPANY_SIZE,
                            Fields.RESPONSIBILITIES, Fields.REQUIRED_QUALIFICATIONS,
                            Fields.PREFERRED_QUALIFICATIONS, Fields.TECH_STACK_PARSED,
                            Fields.IDEAL_CANDIDATE, Fields.MUST_HAVE_SIGNALS,
                            Fields.TECHNICAL_CONTEXT, Fields.PREPARATION_FOCUS
                        )
                    )
                }
            }
            .size(k)
            .build()

        val response = openSearchClient.search(request, Map::class.java)

        return response.hits().hits().mapNotNull { hit ->
            val src = hit.source() ?: return@mapNotNull null
            @Suppress("UNCHECKED_CAST")
            val map = src as Map<String, Any?>
            val id = (map[Fields.ID] as? Number)?.toLong() ?: return@mapNotNull null
            @Suppress("UNCHECKED_CAST")
            val techStackParsed = (map[Fields.TECH_STACK_PARSED] as? List<String>)
            KnnSearchResult(
                id = id,
                score = hit.score()?.toFloat() ?: 0f,
                brandName = map[Fields.BRAND_NAME] as? String ?: "",
                positionName = map[Fields.POSITION_NAME] as? String ?: "",
                companyDomain = map[Fields.COMPANY_DOMAIN] as? String,
                companySize = map[Fields.COMPANY_SIZE] as? String,
                responsibilities = map[Fields.RESPONSIBILITIES] as? String ?: "",
                requiredQualifications = map[Fields.REQUIRED_QUALIFICATIONS] as? String ?: "",
                preferredQualifications = map[Fields.PREFERRED_QUALIFICATIONS] as? String,
                techStackParsed = techStackParsed,
                idealCandidate = map[Fields.IDEAL_CANDIDATE] as? String,
                mustHaveSignals = map[Fields.MUST_HAVE_SIGNALS] as? String,
                technicalContext = map[Fields.TECHNICAL_CONTEXT] as? String,
                preparationFocus = map[Fields.PREPARATION_FOCUS] as? String
            )
        }
    }

    data class KnnSearchResult(
        val id: Long,
        val score: Float,
        val brandName: String,
        val positionName: String,
        val companyDomain: String?,
        val companySize: String?,
        val responsibilities: String,
        val requiredQualifications: String,
        val preferredQualifications: String?,
        val techStackParsed: List<String>?,
        val idealCandidate: String?,
        val mustHaveSignals: String?,
        val technicalContext: String?,
        val preparationFocus: String?
    )

    /**
     * 하이브리드 검색 (kNN 벡터 + 키워드 BM25)
     *
     * 처리 흐름:
     * 1. kNN 쿼리(벡터 유사도)와 multi_match 쿼리(키워드 매칭)를 bool should로 결합
     * 2. 선택적 필터(careerType, companyDomain) 적용
     * 3. topN 결과 반환
     *
     * @param queryVector   쿼리 임베딩 벡터
     * @param keyword       BM25 매칭용 키워드 텍스트
     * @param topN          최종 반환 문서 수
     * @param candidateSize kNN 후보 사이즈
     */
    fun searchHybrid(
        queryVector: List<Float>,
        keyword: String,
        topN: Int,
        candidateSize: Int,
        careerType: String? = null,
        companyDomain: String? = null
    ): List<KnnSearchResult> {
        val filterClauses = buildFilterClauses(careerType, companyDomain)

        val knnQuery = Query.of { qi ->
            qi.knn { knn ->
                knn.field(Fields.EMBEDDING_VECTOR)
                    .vector(queryVector.toFloatArray())
                    .k(candidateSize)
                    .apply {
                        if (filterClauses.isNotEmpty()) {
                            filter(Query.of { fq -> fq.bool { fb -> fb.must(filterClauses) } })
                        }
                    }
            }
        }

        val keywordQuery = Query.of { qi ->
            qi.multiMatch { mm ->
                mm.query(keyword).fields(
                    listOf(Fields.RESPONSIBILITIES, Fields.REQUIRED_QUALIFICATIONS, Fields.PREFERRED_QUALIFICATIONS)
                )
            }
        }

        val request = SearchRequest.Builder()
            .index(INDEX_NAME)
            .query { q -> q.bool { b -> b.should(listOf(knnQuery, keywordQuery)) } }
            .source { s ->
                s.filter { f ->
                    f.includes(
                        listOf(
                            Fields.ID, Fields.BRAND_NAME, Fields.POSITION_NAME,
                            Fields.COMPANY_DOMAIN, Fields.COMPANY_SIZE,
                            Fields.RESPONSIBILITIES, Fields.REQUIRED_QUALIFICATIONS,
                            Fields.PREFERRED_QUALIFICATIONS, Fields.TECH_STACK_PARSED,
                            Fields.IDEAL_CANDIDATE, Fields.MUST_HAVE_SIGNALS,
                            Fields.TECHNICAL_CONTEXT, Fields.PREPARATION_FOCUS
                        )
                    )
                }
            }
            .size(topN)
            .build()

        val response = openSearchClient.search(request, Map::class.java)

        return response.hits().hits().mapNotNull { hit ->
            val src = hit.source() ?: return@mapNotNull null
            @Suppress("UNCHECKED_CAST")
            val map = src as Map<String, Any?>
            val id = (map[Fields.ID] as? Number)?.toLong() ?: return@mapNotNull null
            @Suppress("UNCHECKED_CAST")
            KnnSearchResult(
                id = id,
                score = hit.score()?.toFloat() ?: 0f,
                brandName = map[Fields.BRAND_NAME] as? String ?: "",
                positionName = map[Fields.POSITION_NAME] as? String ?: "",
                companyDomain = map[Fields.COMPANY_DOMAIN] as? String,
                companySize = map[Fields.COMPANY_SIZE] as? String,
                responsibilities = map[Fields.RESPONSIBILITIES] as? String ?: "",
                requiredQualifications = map[Fields.REQUIRED_QUALIFICATIONS] as? String ?: "",
                preferredQualifications = map[Fields.PREFERRED_QUALIFICATIONS] as? String,
                techStackParsed = map[Fields.TECH_STACK_PARSED] as? List<String>,
                idealCandidate = map[Fields.IDEAL_CANDIDATE] as? String,
                mustHaveSignals = map[Fields.MUST_HAVE_SIGNALS] as? String,
                technicalContext = map[Fields.TECHNICAL_CONTEXT] as? String,
                preparationFocus = map[Fields.PREPARATION_FOCUS] as? String
            )
        }
    }

    /**
     * 필드별 terms 집계
     *
     * @param ids          대상 문서 ID 목록 (null이면 전체)
     * @param careerType   필터 (null이면 전체)
     * @param companyDomain 필터 (null이면 전체)
     * @param size         각 aggregation bucket 최대 수
     */
    fun aggregateFields(
        ids: List<Long>?,
        careerType: String? = null,
        companyDomain: String? = null,
        size: Int
    ): AggregationResult {
        val filterClauses = buildList<Query> {
            ids?.let { idList ->
                add(Query.of { qi -> qi.ids { i -> i.values(idList.map { it.toString() }) } })
            }
            addAll(buildFilterClauses(careerType, companyDomain))
        }

        val request = SearchRequest.Builder()
            .index(INDEX_NAME)
            .query { q ->
                if (filterClauses.isEmpty()) q.matchAll { m -> m }
                else q.bool { b -> b.must(filterClauses) }
            }
            .size(0)
            .aggregations(
                mapOf(
                    "techStacks" to Aggregation.of { a -> a.terms { t -> t.field(Fields.TECH_STACK_PARSED).size(size) } },
                    "careerTypes" to Aggregation.of { a -> a.terms { t -> t.field(Fields.CAREER_TYPE).size(size) } },
                    "positionCategories" to Aggregation.of { a -> a.terms { t -> t.field("${Fields.POSITION_CATEGORY_NAME}.${Fields.KEYWORD_SUFFIX}").size(size) } },
                    "companyDomains" to Aggregation.of { a -> a.terms { t -> t.field(Fields.COMPANY_DOMAIN).size(size) } },
                    "companySizes" to Aggregation.of { a -> a.terms { t -> t.field(Fields.COMPANY_SIZE).size(size) } }
                )
            )
            .build()

        val response = openSearchClient.search(request, Map::class.java)

        fun extractTerms(aggName: String): Map<String, Long> =
            response.aggregations()[aggName]
                ?.sterms()?.buckets()?.array()
                ?.associate { it.key() to it.docCount() }
                ?: emptyMap()

        return AggregationResult(
            techStacks = extractTerms("techStacks"),
            careerTypes = extractTerms("careerTypes"),
            positionCategories = extractTerms("positionCategories"),
            companyDomains = extractTerms("companyDomains"),
            companySizes = extractTerms("companySizes")
        )
    }

    /**
     * cohort 문서 텍스트 필드 조회 (STATISTICS textFeature 추출용)
     */
    fun findCohortDocumentTexts(ids: List<Long>): List<RawDocumentFields> {
        if (ids.isEmpty()) return emptyList()

        val request = SearchRequest.Builder()
            .index(INDEX_NAME)
            .query { q -> q.ids { i -> i.values(ids.map { it.toString() }) } }
            .source { s ->
                s.filter { f ->
                    f.includes(
                        listOf(
                            Fields.ID, Fields.RESPONSIBILITIES,
                            Fields.REQUIRED_QUALIFICATIONS, Fields.PREFERRED_QUALIFICATIONS,
                            Fields.TECH_STACK_PARSED
                        )
                    )
                }
            }
            .size(ids.size)
            .build()

        val response = openSearchClient.search(request, Map::class.java)

        return response.hits().hits().mapNotNull { hit ->
            val src = hit.source() ?: return@mapNotNull null
            @Suppress("UNCHECKED_CAST")
            val map = src as Map<String, Any?>
            val id = (map[Fields.ID] as? Number)?.toLong() ?: return@mapNotNull null
            @Suppress("UNCHECKED_CAST")
            RawDocumentFields(
                id = id,
                responsibilities = map[Fields.RESPONSIBILITIES] as? String,
                requiredQualifications = map[Fields.REQUIRED_QUALIFICATIONS] as? String,
                preferredQualifications = map[Fields.PREFERRED_QUALIFICATIONS] as? String,
                techStackParsed = map[Fields.TECH_STACK_PARSED] as? List<String>
            )
        }
    }

    private fun buildFilterClauses(careerType: String?, companyDomain: String?): List<Query> =
        buildList {
            careerType?.let {
                add(Query.of { q -> q.term { t -> t.field(Fields.CAREER_TYPE).value { v -> v.stringValue(it) } } })
            }
            companyDomain?.let {
                add(Query.of { q -> q.term { t -> t.field(Fields.COMPANY_DOMAIN).value { v -> v.stringValue(it) } } })
            }
        }

    data class RawDocumentFields(
        val id: Long,
        val responsibilities: String?,
        val requiredQualifications: String?,
        val preferredQualifications: String?,
        val techStackParsed: List<String>?
    )

    data class AggregationResult(
        val techStacks: Map<String, Long>,
        val careerTypes: Map<String, Long>,
        val positionCategories: Map<String, Long>,
        val companyDomains: Map<String, Long>,
        val companySizes: Map<String, Long>
    )

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
