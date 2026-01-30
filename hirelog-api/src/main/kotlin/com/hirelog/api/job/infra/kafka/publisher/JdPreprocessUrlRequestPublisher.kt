package com.hirelog.api.job.infra.kafka.publisher

import com.hirelog.api.job.application.messaging.JdPreprocessRequestMessage
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * JdPreprocessUrlRequestPublisher
 *
 * 역할:
 * - URL 기반 JD 전처리 요청 Kafka 발행
 */
@Component
class JdPreprocessUrlRequestPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    companion object {
        private const val TOPIC = "jd.preprocess.url.request"
    }

    fun publish(message: JdPreprocessRequestMessage) {
        kafkaTemplate.send(
            TOPIC,
            message.eventId,
            message
        )
    }
}
