package com.hirelog.api.config.infra.webclient

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.http.HttpHeaders
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration
import java.util.concurrent.TimeUnit

object WebClientFactory {

    fun create(
        baseUrl: String,
        poolName: String,
        maxConnections: Int,
        responseTimeoutSec: Long,
        maxInMemorySizeMb: Int,
        userAgent: String
    ): WebClient {

        val provider = ConnectionProvider.builder(poolName)
            .maxConnections(maxConnections)
            .maxIdleTime(Duration.ofSeconds(30))
            .maxLifeTime(Duration.ofMinutes(5))
            .pendingAcquireTimeout(Duration.ofSeconds(10))
            .build()

        val httpClient = HttpClient.create(provider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
            .doOnConnected {
                it.addHandlerLast(ReadTimeoutHandler(responseTimeoutSec, TimeUnit.SECONDS))
                it.addHandlerLast(WriteTimeoutHandler(responseTimeoutSec, TimeUnit.SECONDS))
            }
            .responseTimeout(Duration.ofSeconds(responseTimeoutSec))

        val strategies = ExchangeStrategies.builder()
            .codecs {
                it.defaultCodecs()
                    .maxInMemorySize(maxInMemorySizeMb * 1024 * 1024)
            }
            .build()

        return WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(strategies)
            .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
            .build()
    }
}
