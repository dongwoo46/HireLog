package com.hirelog.api.common.infra.kafka

import org.apache.kafka.clients.producer.ProducerConfig
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

/**
 * KafkaProducerConfig
 *
 * 책임:
 * - HireLog Kafka Producer 설정 구성
 * - spring.kafka 기본 안전 설정 + hirelog.kafka 튜닝 값 결합
 *
 * 설계 원칙:
 * - idempotence 기반 at-least-once
 * - Exactly-once(transactional.id) 미사용
 * - Outbox 패턴과 결합 전제
 */
@Configuration
class KafkaProducerConfig(
    private val springKafkaProperties: KafkaProperties,
    private val hirelogKafkaProperties: HireLogKafkaProperties
) {
    /**
     * ProducerFactory
     *
     * 역할:
     * - Kafka Producer 인스턴스 생성 책임
     * - 설정 조합의 단일 진입점
     */
    @Bean
    fun kafkaProducerFactory(): ProducerFactory<String, Any> {

        // spring.kafka.* 에 정의된 기본 Producer 설정 로딩
        val props = springKafkaProperties.buildProducerProperties().toMutableMap()

        // ===== HireLog Producer 튜닝 값 적용 =====
        props[ProducerConfig.LINGER_MS_CONFIG] =
            hirelogKafkaProperties.producer.lingerMs

        props[ProducerConfig.BATCH_SIZE_CONFIG] =
            hirelogKafkaProperties.producer.batchSize

        props[ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG] =
            hirelogKafkaProperties.producer.deliveryTimeoutMs

        props[ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG] =
            hirelogKafkaProperties.producer.requestTimeoutMs

        return DefaultKafkaProducerFactory(props)
    }

    /**
     * KafkaTemplate
     *
     * 역할:
     * - Application / Adapter 계층에서 사용하는 Kafka 발행 진입점
     *
     * 주의:
     * - 비즈니스 로직 직접 포함 ❌
     * - Outbox Publisher에서만 사용
     */
    @Bean
    fun kafkaTemplate(
        producerFactory: ProducerFactory<String, Any>
    ): KafkaTemplate<String, Any> {
        return KafkaTemplate(producerFactory)
    }
}
