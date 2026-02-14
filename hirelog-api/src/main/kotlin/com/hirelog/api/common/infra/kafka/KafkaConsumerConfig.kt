package com.hirelog.api.common.infra.kafka

import com.hirelog.api.common.application.kafka.FailedKafkaEventService
import com.hirelog.api.job.application.messaging.JdPreprocessFailEvent
import com.hirelog.api.job.application.messaging.JdPreprocessResponseEvent
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.util.backoff.FixedBackOff

/**
 * Kafka Consumer 설정
 *
 * 에러 처리 정책:
 * - 최대 3회 재시도 (1초 간격)
 * - 재시도 실패 시:
 *   1. DB에 실패 기록 저장 (failed_kafka_event)
 *   2. DLT(Dead Letter Topic)로 메시지 전송
 *   3. offset 커밋 (다음 메시지 처리 진행)
 */
@EnableKafka
@Configuration
class KafkaConsumerConfig(
    private val springKafkaProperties: KafkaProperties,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val failedKafkaEventService: FailedKafkaEventService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val RETRY_INTERVAL_MS = 1000L
        private const val MAX_RETRY_COUNT = 3L
        private const val DLT_SUFFIX = ".DLT"
    }

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

        logger.info("Creating jdPreprocessResponseListenerContainerFactory with retry={}, interval={}ms",
            MAX_RETRY_COUNT, RETRY_INTERVAL_MS)

        return ConcurrentKafkaListenerContainerFactory<String, JdPreprocessResponseEvent>().apply {
            consumerFactory = jdPreprocessResponseConsumerFactory
            setBatchListener(false)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            setCommonErrorHandler(createErrorHandler("jd-preprocess-response-consumer"))
            setConcurrency(3)
            setAutoStartup(true)
        }
    }

    /**
     * String 메시지용 ConsumerFactory
     * - Debezium CDC Outbox 메시지 소비용
     */
    @Bean
    fun stringConsumerFactory(): ConsumerFactory<String, String> {
        val props = springKafkaProperties.buildConsumerProperties().toMutableMap()

        return DefaultKafkaConsumerFactory(
            props,
            StringDeserializer(),
            StringDeserializer()
        )
    }

    /**
     * String 메시지용 ListenerContainerFactory
     * - Debezium CDC Outbox 메시지 소비용
     * - Manual Ack 모드
     */
    @Bean
    fun stringListenerContainerFactory(
        stringConsumerFactory: ConsumerFactory<String, String>
    ): ConcurrentKafkaListenerContainerFactory<String, String> {

        logger.info("Creating stringListenerContainerFactory with retry={}, interval={}ms",
            MAX_RETRY_COUNT, RETRY_INTERVAL_MS)

        return ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            consumerFactory = stringConsumerFactory
            setBatchListener(false)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            setCommonErrorHandler(createErrorHandler("string-consumer"))
            setConcurrency(3)
            setAutoStartup(true)
        }
    }

    /**
     * JdPreprocessFail 전용 ConsumerFactory
     * - Python 파이프라인 실패 이벤트 소비용
     */
    @Bean
    fun jdPreprocessFailConsumerFactory(): ConsumerFactory<String, JdPreprocessFailEvent> {
        val props = springKafkaProperties.buildConsumerProperties().toMutableMap()

        val jsonDeserializer = JsonDeserializer(JdPreprocessFailEvent::class.java).apply {
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
     * JdPreprocessFail 전용 ListenerContainerFactory
     * - DLT 전송 없음 (이미 실패 이벤트이므로)
     * - 단순 로깅 후 offset 커밋
     */
    @Bean
    fun jdPreprocessFailListenerContainerFactory(
        jdPreprocessFailConsumerFactory: ConsumerFactory<String, JdPreprocessFailEvent>
    ): ConcurrentKafkaListenerContainerFactory<String, JdPreprocessFailEvent> {

        logger.info("Creating jdPreprocessFailListenerContainerFactory (no DLT)")

        return ConcurrentKafkaListenerContainerFactory<String, JdPreprocessFailEvent>().apply {
            consumerFactory = jdPreprocessFailConsumerFactory
            setBatchListener(false)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            setCommonErrorHandler(createSimpleErrorHandler())
            setConcurrency(1)
            setAutoStartup(true)
        }
    }

    /**
     * 공통 ErrorHandler 생성
     *
     * 동작:
     * 1. FixedBackOff: 1초 간격, 최대 3회 재시도
     * 2. 재시도 실패 시 Recoverer 호출:
     *    - DB에 실패 기록 저장
     *    - DLT로 메시지 전송
     *    - offset 자동 커밋 (DefaultErrorHandler 기본 동작)
     */
    private fun createErrorHandler(consumerGroup: String): DefaultErrorHandler {
        val recoverer = createRecoverer(consumerGroup)
        val backOff = FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRY_COUNT)

        return DefaultErrorHandler(recoverer, backOff).apply {
            // 특정 예외는 재시도하지 않음 (즉시 DLT 전송)
            addNotRetryableExceptions(
                IllegalArgumentException::class.java,
                com.fasterxml.jackson.core.JsonParseException::class.java,
                com.fasterxml.jackson.databind.JsonMappingException::class.java
            )
        }
    }

    /**
     * Recoverer 생성 (DB 저장 + DLT 전송)
     */
    private fun createRecoverer(consumerGroup: String): DeadLetterPublishingRecoverer {
        return DeadLetterPublishingRecoverer(kafkaTemplate) { record, exception ->
            // 1. DB에 실패 기록 저장
            failedKafkaEventService.save(
                topic = record.topic(),
                partition = record.partition(),
                offset = record.offset(),
                key = record.key()?.toString(),
                value = record.value()?.toString(),
                consumerGroup = consumerGroup,
                exception = exception as Exception,
                retryCount = MAX_RETRY_COUNT.toInt()
            )

            logger.error(
                "[KAFKA_CONSUMER_EXHAUSTED] Sending to DLT. topic={}, partition={}, offset={}, consumerGroup={}, exception={}",
                record.topic(),
                record.partition(),
                record.offset(),
                consumerGroup,
                exception.message
            )

            // 2. DLT 토픽으로 전송 (원본토픽.DLT)
            org.apache.kafka.common.TopicPartition(
                record.topic() + DLT_SUFFIX,
                record.partition()
            )
        }
    }

    /**
     * 단순 ErrorHandler 생성 (DLT 없음)
     * - 실패 이벤트 Consumer용
     * - 3회 재시도 후 로깅만 수행
     */
    private fun createSimpleErrorHandler(): DefaultErrorHandler {
        val backOff = FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRY_COUNT)

        return DefaultErrorHandler({ record, exception ->
            logger.error(
                "[JD_PREPROCESS_FAIL_CONSUMER_ERROR] topic={}, partition={}, offset={}, exception={}",
                record.topic(),
                record.partition(),
                record.offset(),
                exception.message,
                exception
            )
        }, backOff).apply {
            addNotRetryableExceptions(
                IllegalArgumentException::class.java,
                com.fasterxml.jackson.core.JsonParseException::class.java,
                com.fasterxml.jackson.databind.JsonMappingException::class.java
            )
        }
    }
}
