package com.hirelog.api.common.infra.opensearch

import com.hirelog.api.common.config.properties.OpenSearchProperties
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.transport.rest_client.RestClientTransport
import org.opensearch.client.RestClient
import org.apache.http.HttpHost
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(OpenSearchProperties::class)
class OpenSearchConfig(
    private val properties: OpenSearchProperties,
) {

    @Bean
    fun openSearchClient(): OpenSearchClient {

        // OpenSearch가 공식적으로 노출하는 RestClient 사용
        val restClient = RestClient.builder(
            HttpHost(properties.host, properties.port, properties.scheme)
        ).build()

        val transport = RestClientTransport(
            restClient,
            JacksonJsonpMapper()
        )

        return OpenSearchClient(transport)
    }
}
