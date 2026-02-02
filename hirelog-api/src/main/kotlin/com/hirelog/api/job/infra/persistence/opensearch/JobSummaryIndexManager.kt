package com.hirelog.api.job.infra.persistence.opensearch

import com.hirelog.api.common.logging.log
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryIndexConstants.Analyzers
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryIndexConstants.Fields
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryIndexConstants.INDEX_NAME
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.analysis.Analyzer
import org.opensearch.client.opensearch._types.analysis.CustomAnalyzer
import org.opensearch.client.opensearch._types.mapping.DateProperty
import org.opensearch.client.opensearch._types.mapping.KeywordProperty
import org.opensearch.client.opensearch._types.mapping.LongNumberProperty
import org.opensearch.client.opensearch._types.mapping.Property
import org.opensearch.client.opensearch._types.mapping.TextProperty
import org.opensearch.client.opensearch._types.mapping.TypeMapping
import org.opensearch.client.opensearch.indices.CreateIndexRequest
import org.opensearch.client.opensearch.indices.IndexSettings
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis
import org.springframework.stereotype.Component

/**
 * JobSummary OpenSearch 인덱스 관리
 *
 * 책임:
 * - 인덱스 생성/삭제
 * - 매핑 정의
 *
 * 설계:
 * - nori 분석기: 한글 형태소 분석
 * - english 분석기: 영어 표준 분석
 * - multi-field: 동일 필드에 한글/영어 분석기 모두 적용
 */
@Component
class JobSummaryIndexManager(
    private val openSearchClient: OpenSearchClient
) {

    /**
     * 인덱스 존재 여부 확인
     */
    fun existsIndex(): Boolean {
        return openSearchClient.indices()
            .exists { it.index(INDEX_NAME) }
            .value()
    }

    /**
     * 인덱스 생성
     *
     * 멱등성:
     * - 이미 존재하면 생성하지 않음
     */
    fun createIndexIfNotExists() {
        if (existsIndex()) {
            log.info("Index already exists: {}", INDEX_NAME)
            return
        }

        val request = CreateIndexRequest.Builder()
            .index(INDEX_NAME)
            .settings(buildSettings())
            .mappings(buildMappings())
            .build()

        openSearchClient.indices().create(request)
        log.info("Index created: {}", INDEX_NAME)
    }

    /**
     * 인덱스 삭제 (테스트/재생성용)
     */
    fun deleteIndex() {
        if (!existsIndex()) {
            log.info("Index does not exist: {}", INDEX_NAME)
            return
        }

        openSearchClient.indices().delete { it.index(INDEX_NAME) }
        log.info("Index deleted: {}", INDEX_NAME)
    }

    /**
     * 인덱스 설정 빌드
     */
    private fun buildSettings(): IndexSettings {
        return IndexSettings.Builder()
            .numberOfShards("1")
            .numberOfReplicas("0")
            .analysis(buildAnalysis())
            .build()
    }

    /**
     * 분석기 설정
     */
    private fun buildAnalysis(): IndexSettingsAnalysis {
        return IndexSettingsAnalysis.Builder()
            .analyzer(Analyzers.NORI, Analyzer.Builder()
                .custom(CustomAnalyzer.Builder()
                    .tokenizer("nori_tokenizer")
                    .filter("lowercase", "nori_readingform")
                    .build())
                .build())
            .analyzer(Analyzers.ENGLISH, Analyzer.Builder()
                .custom(CustomAnalyzer.Builder()
                    .tokenizer("standard")
                    .filter("lowercase", "porter_stem")
                    .build())
                .build())
            .build()
    }

    /**
     * 매핑 빌드
     */
    private fun buildMappings(): TypeMapping {
        return TypeMapping.Builder()
            // === ID 필드 (long) ===
            .properties(Fields.ID, longProperty())
            .properties(Fields.JOB_SNAPSHOT_ID, longProperty())
            .properties(Fields.BRAND_ID, longProperty())
            .properties(Fields.COMPANY_ID, longProperty())
            .properties(Fields.POSITION_ID, longProperty())

            // === 검색 대상 텍스트 필드 (한글/영어 multi-field) ===
            .properties(Fields.BRAND_NAME, searchableTextField())
            .properties(Fields.COMPANY_NAME, searchableTextField())
            .properties(Fields.POSITION_NAME, searchableTextField())
            .properties(Fields.BRAND_POSITION_NAME, searchableTextField())
            .properties(Fields.SUMMARY_TEXT, searchableTextField())
            .properties(Fields.RESPONSIBILITIES, searchableTextField())
            .properties(Fields.REQUIRED_QUALIFICATIONS, searchableTextField())
            .properties(Fields.PREFERRED_QUALIFICATIONS, searchableTextField())
            .properties(Fields.TECH_STACK, searchableTextField())
            .properties(Fields.RECRUITMENT_PROCESS, searchableTextField())

            // === Insight 필드 (검색 가능) ===
            .properties(Fields.IDEAL_CANDIDATE, searchableTextField())
            .properties(Fields.MUST_HAVE_SIGNALS, searchableTextField())
            .properties(Fields.PREPARATION_FOCUS, searchableTextField())
            .properties(Fields.TRANSFERABLE_STRENGTHS_AND_GAP_PLAN, searchableTextField())
            .properties(Fields.PROOF_POINTS_AND_METRICS, searchableTextField())
            .properties(Fields.STORY_ANGLES, searchableTextField())
            .properties(Fields.KEY_CHALLENGES, searchableTextField())
            .properties(Fields.TECHNICAL_CONTEXT, searchableTextField())
            .properties(Fields.QUESTIONS_TO_ASK, searchableTextField())
            .properties(Fields.CONSIDERATIONS, searchableTextField())

            // === 기술스택 파싱 배열 (keyword) ===
            .properties(Fields.TECH_STACK_PARSED, keywordProperty())

            // === 필터 필드 (keyword) ===
            .properties(Fields.CAREER_TYPE, keywordProperty())
            .properties(Fields.CAREER_YEARS, keywordProperty())

            // === 날짜 필드 ===
            .properties(Fields.CREATED_AT, dateProperty())

            .build()
    }

    private fun longProperty(): Property {
        return Property.of { p -> p.long_(LongNumberProperty.Builder().build()) }
    }

    private fun keywordProperty(): Property {
        return Property.of { p -> p.keyword(KeywordProperty.Builder().build()) }
    }

    private fun dateProperty(): Property {
        return Property.of { p ->
            p.date(DateProperty.Builder()
                .format("yyyy-MM-dd'T'HH:mm:ss||yyyy-MM-dd||epoch_millis")
                .build())
        }
    }

    /**
     * 검색 가능한 텍스트 필드 빌드
     *
     * 구조:
     * - 기본: nori_analyzer (한글)
     * - .english: english_analyzer (영어)
     * - .keyword: keyword (정확한 매칭/집계)
     */
    private fun searchableTextField(): Property {
        return Property.of { p ->
            p.text(TextProperty.Builder()
                .analyzer(Analyzers.NORI)
                .fields(Fields.ENGLISH_SUFFIX, Property.of { sub ->
                    sub.text(TextProperty.Builder()
                        .analyzer(Analyzers.ENGLISH)
                        .build())
                })
                .fields(Fields.KEYWORD_SUFFIX, Property.of { sub ->
                    sub.keyword(KeywordProperty.Builder()
                        .ignoreAbove(256)
                        .build())
                })
                .build())
        }
    }
}
