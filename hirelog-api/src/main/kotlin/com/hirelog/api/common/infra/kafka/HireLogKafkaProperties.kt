package com.hirelog.api.common.infra.kafka

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "hirelog.kafka")
data class HireLogKafkaProperties(
    val producer: Producer,
    val consumer: Consumer
) {
    data class Producer(
        val lingerMs: Int,
        val batchSize: Int,
        val deliveryTimeoutMs: Int,
        val requestTimeoutMs: Int
    )

    data class Consumer(
        val maxPollRecords: Int,
        val maxPollIntervalMs: Int,
        val sessionTimeoutMs: Int
    )
}
