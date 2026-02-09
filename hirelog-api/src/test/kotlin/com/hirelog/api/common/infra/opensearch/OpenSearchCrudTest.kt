package com.hirelog.api.common.infra.opensearch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.Refresh
import org.opensearch.client.opensearch.core.*
import org.opensearch.client.opensearch.indices.CreateIndexRequest
import org.opensearch.client.opensearch.indices.DeleteIndexRequest
import org.opensearch.client.opensearch.indices.ExistsRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@DisplayName("OpenSearch 연결 및 CRUD 테스트")
class OpenSearchCrudTest {

    @Autowired
    private lateinit var openSearchClient: OpenSearchClient

    companion object {
        private const val TEST_INDEX = "test-index"
    }

    @BeforeEach
    fun setUp() {
        // 테스트 인덱스가 존재하면 삭제
        deleteIndexIfExists()
        // 테스트 인덱스 생성
        createTestIndex()
    }

    @AfterEach
    fun tearDown() {
        // 테스트 후 인덱스 삭제
        deleteIndexIfExists()
    }

    @Test
    @Order(1)
    @DisplayName("OpenSearch 클라이언트 연결을 확인한다")
    fun shouldConnectToOpenSearch() {
        // when
        val info = openSearchClient.info()

        // then
        assertThat(info).isNotNull
        assertThat(info.version()).isNotNull
        println("OpenSearch Version: ${info.version().number()}")
    }

    @Test
    @Order(2)
    @DisplayName("문서를 생성한다 (Create)")
    fun shouldCreateDocument() {
        // given
        val documentId = "test-doc-1"
        val document = TestDocument(
            id = documentId,
            title = "테스트 문서",
            content = "OpenSearch 연결 테스트",
            tags = listOf("test", "opensearch")
        )

        // when
        val response = openSearchClient.index { i ->
            i.index(TEST_INDEX)
                .id(documentId)
                .document(document)
                .refresh(Refresh.True) // 즉시 검색 가능하도록
        }

        // then
        assertThat(response.result().jsonValue()).isEqualTo("created")
        assertThat(response.id()).isEqualTo(documentId)
    }

    @Test
    @Order(3)
    @DisplayName("문서를 조회한다 (Read)")
    fun shouldReadDocument() {
        // given
        val documentId = "test-doc-2"
        val document = TestDocument(
            id = documentId,
            title = "조회 테스트",
            content = "문서 조회 테스트",
            tags = listOf("read")
        )

        // 문서 생성
        openSearchClient.index { i ->
            i.index(TEST_INDEX)
                .id(documentId)
                .document(document)
                .refresh(Refresh.True)
        }

        // when
        val response = openSearchClient.get({ g ->
            g.index(TEST_INDEX)
                .id(documentId)
        }, TestDocument::class.java)

        // then
        assertThat(response.found()).isTrue()
        assertThat(response.source()).isNotNull
        assertThat(response.source()?.title).isEqualTo("조회 테스트")
        assertThat(response.source()?.content).isEqualTo("문서 조회 테스트")
        assertThat(response.source()?.tags).containsExactly("read")
    }

    @Test
    @Order(4)
    @DisplayName("문서를 수정한다 (Update)")
    fun shouldUpdateDocument() {
        // given
        val documentId = "test-doc-3"
        val originalDocument = TestDocument(
            id = documentId,
            title = "원본 제목",
            content = "원본 내용",
            tags = listOf("original")
        )

        // 원본 문서 생성
        openSearchClient.index { i ->
            i.index(TEST_INDEX)
                .id(documentId)
                .document(originalDocument)
                .refresh(Refresh.True)
        }

        // when - 문서 수정
        val updatedDocument = TestDocument(
            id = documentId,
            title = "수정된 제목",
            content = "수정된 내용",
            tags = listOf("updated", "modified")
        )

        val updateResponse = openSearchClient.update({ u ->
            u.index(TEST_INDEX)
                .id(documentId)
                .doc(updatedDocument)
                .refresh(Refresh.True)
        }, TestDocument::class.java)

        // then
        assertThat(updateResponse.result().jsonValue()).isEqualTo("updated")

        // 수정된 문서 조회하여 확인
        val getResponse = openSearchClient.get({ g ->
            g.index(TEST_INDEX)
                .id(documentId)
        }, TestDocument::class.java)

        assertThat(getResponse.source()?.title).isEqualTo("수정된 제목")
        assertThat(getResponse.source()?.content).isEqualTo("수정된 내용")
        assertThat(getResponse.source()?.tags).containsExactly("updated", "modified")
    }

    @Test
    @Order(5)
    @DisplayName("문서를 삭제한다 (Delete)")
    fun shouldDeleteDocument() {
        // given
        val documentId = "test-doc-4"
        val document = TestDocument(
            id = documentId,
            title = "삭제 테스트",
            content = "문서 삭제 테스트",
            tags = listOf("delete")
        )

        // 문서 생성
        openSearchClient.index { i ->
            i.index(TEST_INDEX)
                .id(documentId)
                .document(document)
                .refresh(Refresh.True)
        }

        // when - 문서 삭제
        val deleteResponse = openSearchClient.delete { d ->
            d.index(TEST_INDEX)
                .id(documentId)
                .refresh(Refresh.True)
        }

        // then
        assertThat(deleteResponse.result().jsonValue()).isEqualTo("deleted")

        // 삭제된 문서 조회 시도
        val getResponse = openSearchClient.get({ g ->
            g.index(TEST_INDEX)
                .id(documentId)
        }, TestDocument::class.java)

        assertThat(getResponse.found()).isFalse()
    }

    @Test
    @Order(6)
    @DisplayName("여러 문서를 검색한다 (Search)")
    fun shouldSearchDocuments() {
        // given - 여러 문서 생성
        val documents = listOf(
            TestDocument("doc-1", "검색 테스트 1", "첫 번째 문서", listOf("search", "test")),
            TestDocument("doc-2", "검색 테스트 2", "두 번째 문서", listOf("search", "example")),
            TestDocument("doc-3", "다른 제목", "세 번째 문서", listOf("other"))
        )

        documents.forEach { doc ->
            openSearchClient.index { i ->
                i.index(TEST_INDEX)
                    .id(doc.id)
                    .document(doc)
                    .refresh(Refresh.True)
            }
        }

        // when - "검색 테스트"를 포함하는 문서 검색
        val searchResponse = openSearchClient.search({ s ->
            s.index(TEST_INDEX)
                .query { q ->
                    q.match { m ->
                        m.field("title")
                            .query { fq -> fq.stringValue("검색 테스트") }
                    }
                }
        }, TestDocument::class.java)

        // then
        assertThat(searchResponse.hits().total()?.value()).isEqualTo(2)
        assertThat(searchResponse.hits().hits()).hasSize(2)

        val titles = searchResponse.hits().hits()
            .mapNotNull { it.source()?.title }

        assertThat(titles).containsExactlyInAnyOrder("검색 테스트 1", "검색 테스트 2")
    }

    @Test
    @Order(7)
    @DisplayName("벌크 작업을 수행한다 (Bulk)")
    fun shouldPerformBulkOperations() {
        // given
        val documents = (1..5).map { i ->
            TestDocument(
                id = "bulk-doc-$i",
                title = "벌크 문서 $i",
                content = "벌크 테스트 내용 $i",
                tags = listOf("bulk", "test-$i")
            )
        }

        // when - 벌크 생성
        val bulkResponse = openSearchClient.bulk { b ->
            documents.forEach { doc ->
                b.operations { op ->
                    op.index { idx ->
                        idx.index(TEST_INDEX)
                            .id(doc.id)
                            .document(doc)
                    }
                }
            }
            b.refresh(Refresh.True)
        }

        // then
        assertThat(bulkResponse.errors()).isFalse()
        assertThat(bulkResponse.items()).hasSize(5)

        // 생성된 문서 확인
        val searchResponse = openSearchClient.search({ s ->
            s.index(TEST_INDEX)
                .query { q ->
                    q.matchAll { ma -> ma }
                }
        }, TestDocument::class.java)

        assertThat(searchResponse.hits().total()?.value()).isEqualTo(5)
    }

    private fun createTestIndex() {
        val request = CreateIndexRequest.Builder()
            .index(TEST_INDEX)
            .build()

        openSearchClient.indices().create(request)
    }

    private fun deleteIndexIfExists() {
        val existsRequest = ExistsRequest.Builder()
            .index(TEST_INDEX)
            .build()

        val exists = openSearchClient.indices().exists(existsRequest).value()

        if (exists) {
            val deleteRequest = DeleteIndexRequest.Builder()
                .index(TEST_INDEX)
                .build()

            openSearchClient.indices().delete(deleteRequest)
        }
    }

    /**
     * 테스트용 문서 클래스
     */
    data class TestDocument(
        val id: String = "",
        val title: String = "",
        val content: String = "",
        val tags: List<String> = emptyList()
    )
}