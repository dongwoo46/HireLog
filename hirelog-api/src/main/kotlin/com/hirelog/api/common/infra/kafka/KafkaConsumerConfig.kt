package com.hirelog.api.common.infra.kafka

import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

/**
 * KafkaConsumerConfig
 *
 * 책임:
 * - HireLog Kafka Consumer 공통 설정 구성
 * - 수동 커밋 + 단건 처리 + retry 없는 소비 모델 확정
 */
@Configuration
class KafkaConsumerConfig(
    private val springKafkaProperties: KafkaProperties,
    private val hirelogKafkaProperties: HireLogKafkaProperties
) {

    /**
     * ConsumerFactory
     *
     * 역할:
     * - spring.kafka 기본 Consumer 설정 로딩
     * - enable-auto-commit=false 전제
     */
    @Bean
    fun kafkaConsumerFactory(): ConsumerFactory<String, Any> {
        val props = springKafkaProperties
            .buildConsumerProperties()
            .toMutableMap()

        return DefaultKafkaConsumerFactory(props)
    }

    /**
     * KafkaListenerContainerFactory
     *
     * 역할:
     * - Listener 실행 방식 정의
     * - AckMode / ErrorHandler / concurrency 결정
     */
    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, Any>
    ): ConcurrentKafkaListenerContainerFactory<String, Any> {

        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()

        factory.consumerFactory = consumerFactory

        // ===== 처리 모델 =====

        // 단건 처리 (batch listener 금지)
        factory.setBatchListener(false)

        // 수동 커밋 (DB 트랜잭션 성공 이후 ack)
        factory.containerProperties.ackMode =
            ContainerProperties.AckMode.MANUAL

        // ===== 실패 전략 =====

        // retry 없음 + offset 유지 (lag 발생 → 운영 개입)
        factory.setCommonErrorHandler(
            DefaultErrorHandler(
                FixedBackOff(0L, 0L)
            )
        )

        // ===== concurrency =====
        // 초기값: partition 1개 기준
        factory.setConcurrency(1)

        return factory
    }
}
