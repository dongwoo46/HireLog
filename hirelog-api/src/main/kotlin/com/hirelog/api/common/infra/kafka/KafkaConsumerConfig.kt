package com.hirelog.api.common.infra.kafka

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

@EnableKafka
@Configuration
class KafkaConsumerConfig(
    private val springKafkaProperties: KafkaProperties,
    private val hirelogKafkaProperties: HireLogKafkaProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        logger.info("🔥 KafkaConsumerConfig INITIALIZED!")
    }

    @Bean
    fun kafkaConsumerFactory(): ConsumerFactory<String, Any> {
        val props = springKafkaProperties
            .buildConsumerProperties()
            .toMutableMap()

        logger.info("🔥 Creating ConsumerFactory with props: $props")
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, Any>
    ): ConcurrentKafkaListenerContainerFactory<String, Any> {

        logger.info("🔥 Creating KafkaListenerContainerFactory")

        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = consumerFactory
        factory.setBatchListener(false)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        factory.setCommonErrorHandler(DefaultErrorHandler(FixedBackOff(0L, 0L)))
        factory.setConcurrency(1)

        factory.setAutoStartup(true)  // 👈 명시적으로 추가!


        logger.info("🔥 KafkaListenerContainerFactory created successfully")
        return factory
    }
}