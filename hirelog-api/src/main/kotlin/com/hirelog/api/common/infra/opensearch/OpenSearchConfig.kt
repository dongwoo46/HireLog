package com.hirelog.api.common.infra.opensearch

import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.ssl.SSLContexts
import org.opensearch.client.RestClient
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.rest_client.RestClientTransport
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

        // 1. BasicAuth 설정
        val credentialsProvider = BasicCredentialsProvider().apply {
            setCredentials(
                AuthScope.ANY,
                UsernamePasswordCredentials(
                    properties.username,
                    properties.password
                )
            )
        }

        // 2. Self-signed 인증서 허용 (로컬 전용)
        val sslContext = SSLContexts.custom()
            .loadTrustMaterial(null) { _, _ -> true }
            .build()

        // 3. HTTPS + 인증 + SSL override 적용
        val restClient = RestClient.builder(
            HttpHost(properties.host, properties.port, properties.scheme)
        )
            .setHttpClientConfigCallback { httpClientBuilder ->
                httpClientBuilder
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .setDefaultCredentialsProvider(credentialsProvider)
            }
            .build()

        val transport = RestClientTransport(
            restClient,
            JacksonJsonpMapper()
        )

        return OpenSearchClient(transport)
    }
}
