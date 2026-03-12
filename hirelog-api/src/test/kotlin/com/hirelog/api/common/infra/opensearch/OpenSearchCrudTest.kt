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
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(
    classes = [OpenSearchConfig::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@DisplayName("OpenSearch 연결 및 CRUD 테스트")
class OpenSearchCrudTest {

    companion object {
        @Container
        @JvmStatic
        val opensearch = GenericContainer("opensearchproject/opensearch:2.11.0")
            .withExposedPorts(9200)
            .withEnv("discovery.type", "single-node")
            .withEnv("DISABLE_SECURITY_PLUGIN", "true")

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("opensearch.host") { opensearch.host }
            registry.add("opensearch.port") { opensearch.getMappedPort(9200) }
            registry.add("opensearch.scheme") { "http" }
            registry.add("opensearch.username") { "" }
            registry.add("opensearch.password") { "" }
        }

        private const val TEST_INDEX = "test-index"
    }

    @Autowired
    private lateinit var openSearchClient: OpenSearchClient

    @BeforeEach
    fun setUp() {
        deleteIndexIfExists()
        createTestIndex()
    }

    @AfterEach
    fun tearDown() {
        deleteIndexIfExists()
    }

    @Test
    @Order(1)
    @DisplayName("OpenSearch 클라이언트 연결을 확인한다")
    fun shouldConnectToOpenSearch() {
        val info = openSearchClient.info()

        assertThat(info).isNotNull
        assertThat(info.version()).isNotNull
        println("OpenSearch Version: ${info.version().number()}")
    }

    @Test
    @Order(2)
    @DisplayName("문서를 생성한다 (Create)")
    fun shouldCreateDocument() {
        val documentId = "test-doc-1"
        val document = TestDocument(
            id = documentId,
            title = "테스트 문서",
            content = "OpenSearch 연결 테스트",
            tags = listOf("test", "opensearch")
        )

        val response = openSearchClient.index { i ->
            i.index(TEST_INDEX)
                .id(documentId)
                .document(document)
                .refresh(Refresh.True)
        }

        assertThat(response.result().jsonValue()).isEqualTo("created")
        assertThat(response.id()).isEqualTo(documentId)
    }

    @Test
    @Order(3)
    @DisplayName("문서를 조회한다 (Read)")
    fun shouldReadDocument() {
        val documentId = "test-doc-2"
        val document = TestDocument(
            id = documentId,
            title = "조회 테스트",
            content = "문서 조회 테스트",
            tags = listOf("read")
        )

        openSearchClient.index { i ->
            i.index(TEST_INDEX)
                .id(documentId)
                .document(document)
                .refresh(Refresh.True)
        }

        val response = openSearchClient.get({ g ->
            g.index(TEST_INDEX).id(documentId)
        }, TestDocument::class.java)

        assertThat(response.found()).isTrue()
        assertThat(response.source()?.title).isEqualTo("조회 테스트")
        assertThat(response.source()?.content).isEqualTo("문서 조회 테스트")
        assertThat(response.source()?.tags).containsExactly("read")
    }

    @Test
    @Order(4)
    @DisplayName("문서를 수정한다 (Update)")
    fun shouldUpdateDocument() {
        val documentId = "test-doc-3"
        openSearchClient.index { i ->
            i.index(TEST_INDEX)
                .id(documentId)
                .document(TestDocument(id = documentId, title = "원본 제목", content = "원본 내용", tags = listOf("original")))
                .refresh(Refresh.True)
        }

        val updateResponse = openSearchClient.update({ u ->
            u.index(TEST_INDEX)
                .id(documentId)
                .doc(TestDocument(id = documentId, title = "수정된 제목", content = "수정된 내용", tags = listOf("updated", "modified")))
                .refresh(Refresh.True)
        }, TestDocument::class.java)

        assertThat(updateResponse.result().jsonValue()).isEqualTo("updated")

        val getResponse = openSearchClient.get({ g ->
            g.index(TEST_INDEX).id(documentId)
        }, TestDocument::class.java)

        assertThat(getResponse.source()?.title).isEqualTo("수정된 제목")
        assertThat(getResponse.source()?.content).isEqualTo("수정된 내용")
        assertThat(getResponse.source()?.tags).containsExactly("updated", "modified")
    }

    @Test
    @Order(5)
    @DisplayName("문서를 삭제한다 (Delete)")
    fun shouldDeleteDocument() {
        val documentId = "test-doc-4"
        openSearchClient.index { i ->
            i.index(TEST_INDEX)
                .id(documentId)
                .document(TestDocument(id = documentId, title = "삭제 테스트", content = "문서 삭제 테스트", tags = listOf("delete")))
                .refresh(Refresh.True)
        }

        val deleteResponse = openSearchClient.delete { d ->
            d.index(TEST_INDEX).id(documentId).refresh(Refresh.True)
        }

        assertThat(deleteResponse.result().jsonValue()).isEqualTo("deleted")

        val getResponse = openSearchClient.get({ g ->
            g.index(TEST_INDEX).id(documentId)
        }, TestDocument::class.java)

        assertThat(getResponse.found()).isFalse()
    }

    @Test
    @Order(6)
    @DisplayName("여러 문서를 검색한다 (Search)")
    fun shouldSearchDocuments() {
        listOf(
            TestDocument("doc-1", "검색 테스트 1", "첫 번째 문서", listOf("search", "test")),
            TestDocument("doc-2", "검색 테스트 2", "두 번째 문서", listOf("search", "example")),
            TestDocument("doc-3", "다른 제목", "세 번째 문서", listOf("other"))
        ).forEach { doc ->
            openSearchClient.index { i ->
                i.index(TEST_INDEX).id(doc.id).document(doc).refresh(Refresh.True)
            }
        }

        val searchResponse = openSearchClient.search({ s ->
            s.index(TEST_INDEX)
                .query { q ->
                    q.match { m ->
                        m.field("title").query { fq -> fq.stringValue("검색 테스트") }
                    }
                }
        }, TestDocument::class.java)

        assertThat(searchResponse.hits().total()?.value()).isEqualTo(2)
        assertThat(searchResponse.hits().hits().mapNotNull { it.source()?.title })
            .containsExactlyInAnyOrder("검색 테스트 1", "검색 테스트 2")
    }

    @Test
    @Order(7)
    @DisplayName("벌크 작업을 수행한다 (Bulk)")
    fun shouldPerformBulkOperations() {
        val documents = (1..5).map { i ->
            TestDocument(id = "bulk-doc-$i", title = "벌크 문서 $i", content = "벌크 테스트 내용 $i", tags = listOf("bulk", "test-$i"))
        }

        val bulkResponse = openSearchClient.bulk { b ->
            documents.forEach { doc ->
                b.operations { op ->
                    op.index { idx -> idx.index(TEST_INDEX).id(doc.id).document(doc) }
                }
            }
            b.refresh(Refresh.True)
        }

        assertThat(bulkResponse.errors()).isFalse()
        assertThat(bulkResponse.items()).hasSize(5)

        val searchResponse = openSearchClient.search({ s ->
            s.index(TEST_INDEX).query { q -> q.matchAll { ma -> ma } }
        }, TestDocument::class.java)

        assertThat(searchResponse.hits().total()?.value()).isEqualTo(5)
    }

    private fun createTestIndex() {
        openSearchClient.indices().create(CreateIndexRequest.Builder().index(TEST_INDEX).build())
    }

    private fun deleteIndexIfExists() {
        val exists = openSearchClient.indices().exists(ExistsRequest.Builder().index(TEST_INDEX).build()).value()
        if (exists) {
            openSearchClient.indices().delete(DeleteIndexRequest.Builder().index(TEST_INDEX).build())
        }
    }

    data class TestDocument(
        val id: String = "",
        val title: String = "",
        val content: String = "",
        val tags: List<String> = emptyList()
    )
}