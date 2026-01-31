package com.hirelog.api.job.infra.kafka.publisher

import com.hirelog.api.job.application.messaging.JdPreprocessRequestMessage
import com.hirelog.api.job.infra.kafka.topic.JdKafkaTopics
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * JdPreprocessOcrRequestPublisher
 *
 * 역할:
 * - OCR 기반 JD 전처리 요청 Kafka 발행
 */
@Component
class JdPreprocessOcrRequestPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    companion object {
        private const val TOPIC = JdKafkaTopics.PREPROCESS_OCR_REQUEST
    }

    fun publish(message: JdPreprocessRequestMessage) {
        kafkaTemplate.send(
            TOPIC,
            message.eventId,
            message
        )
    }
}
