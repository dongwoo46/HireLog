package com.hirelog.api.common.infra.kafka

import com.hirelog.api.job.application.messaging.JdPreprocessResponseEvent
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
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
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.util.backoff.FixedBackOff

@EnableKafka
@Configuration
class KafkaConsumerConfig(
    private val springKafkaProperties: KafkaProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * JdPreprocessResponse 전용 ConsumerFactory
     */
    @Bean
    fun jdPreprocessResponseConsumerFactory(): ConsumerFactory<String, JdPreprocessResponseEvent> {
        val props = springKafkaProperties.buildConsumerProperties().toMutableMap()

        val jsonDeserializer = JsonDeserializer(JdPreprocessResponseEvent::class.java).apply {
            setUseTypeHeaders(false)
            addTrustedPackages("*")
        }

        return DefaultKafkaConsumerFactory(
            props,
            StringDeserializer(),
            jsonDeserializer
        )
    }

    /**
     * JdPreprocessResponse 전용 ListenerContainerFactory
     */
    @Bean
    fun jdPreprocessResponseListenerContainerFactory(
        jdPreprocessResponseConsumerFactory: ConsumerFactory<String, JdPreprocessResponseEvent>
    ): ConcurrentKafkaListenerContainerFactory<String, JdPreprocessResponseEvent> {

        logger.info("Creating jdPreprocessResponseListenerContainerFactory")

        return ConcurrentKafkaListenerContainerFactory<String, JdPreprocessResponseEvent>().apply {
            consumerFactory = jdPreprocessResponseConsumerFactory
            setBatchListener(false)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            setCommonErrorHandler(DefaultErrorHandler(FixedBackOff(0L, 0L)))
            setConcurrency(1)
            setAutoStartup(true)
        }
    }
}
