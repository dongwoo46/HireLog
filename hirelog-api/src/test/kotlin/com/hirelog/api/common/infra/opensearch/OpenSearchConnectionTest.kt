package com.hirelog.api.common.infra.opensearch

import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertTrue

@SpringBootTest
class OpenSearchConnectionTest {

    @Autowired
    private lateinit var openSearchClient: OpenSearchClient

    @Test
    fun `OpenSearch 연결 테스트`() {
        // When
        val response = openSearchClient.ping()

        // Then
        assertTrue(response.value(), "OpenSearch 서버에 연결할 수 없습니다")
        println("OpenSearch 연결 성공!")
    }

    @Test
    fun `OpenSearch 클러스터 정보 조회`() {
        // When
        val info = openSearchClient.info()

        // Then
        println("클러스터 이름: ${info.clusterName()}")
        println("OpenSearch 버전: ${info.version().number()}")
        println("연결 성공!")
    }
}