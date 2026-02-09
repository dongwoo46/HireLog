package com.hirelog.api.common.infra.opensearch

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.core.IndexRequest
import org.opensearch.client.opensearch.core.SearchRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

data class TestDocument @JsonCreator constructor(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("title")
    val title: String,

    @JsonProperty("content")
    val content: String,
)

@SpringBootTest
class OpenSearchCrudTest {

    @Autowired
    private lateinit var openSearchClient: OpenSearchClient

    private val testIndexName = "test-index"

    @AfterEach
    fun cleanup() {
        try {
            openSearchClient.indices().delete { it.index(testIndexName) }
        } catch (e: Exception) {
            // 인덱스가 없으면 무시
        }
    }

    @Test
    fun `문서 색인 및 조회 테스트`() {
        // Given
        val document = TestDocument(
            id = "1",
            title = "테스트 문서",
            content = "OpenSearch 연결 테스트"
        )

        // When - 문서 색인
        val indexResponse = openSearchClient.index(
            IndexRequest.of<TestDocument> { i ->
                i.index(testIndexName)
                    .id(document.id)
                    .document(document)
            }
        )

        // Then
        assertEquals("created", indexResponse.result().jsonValue())
        println("문서 색인 성공: ${indexResponse.id()}")

        // When - 문서 조회
        Thread.sleep(1000) // refresh 대기

        val getResponse = openSearchClient.get(
            { g -> g.index(testIndexName).id(document.id) },
            TestDocument::class.java
        )

        // Then
        assertTrue(getResponse.found())
        assertEquals(document.title, getResponse.source()?.title)
        println("문서 조회 성공: ${getResponse.source()}")
    }

    @Test
    fun `문서 검색 테스트`() {
        // Given
        val documents = listOf(
            TestDocument("1", "Kotlin", "Kotlin is awesome"),
            TestDocument("2", "Java", "Java is great"),
            TestDocument("3", "Kotlin Spring", "Kotlin with Spring Boot")
        )

        documents.forEach { doc ->
            openSearchClient.index(
                IndexRequest.of<TestDocument> { i ->
                    i.index(testIndexName)
                        .id(doc.id)
                        .document(doc)
                }
            )
        }

        Thread.sleep(1000) // refresh 대기

        // When - "Kotlin" 검색
        val searchResponse = openSearchClient.search(
            SearchRequest.of { s ->
                s.index(testIndexName)
                    .query { q ->
                        q.match { m ->
                            m.field("title")
                                .query { v -> v.stringValue("Kotlin") }
                        }
                    }
            },
            TestDocument::class.java
        )

        // Then
        val hits = searchResponse.hits().hits()
        assertTrue(hits.size >= 2, "Kotlin이 포함된 문서가 2개 이상이어야 합니다")
        println("검색된 문서 수: ${hits.size}")
        hits.forEach { hit ->
            println("- ${hit.source()?.title}")
        }
    }
}