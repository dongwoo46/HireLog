package com.hirelog.api.job.infra.kafka.publisher

import com.hirelog.api.job.application.messaging.JdPreprocessRequestMessage
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * JdPreprocessTextRequestPublisher
 *
 * 역할:
 * - TEXT 기반 JD 전처리 요청 Kafka 발행
 */
@Component
class JdPreprocessTextRequestPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    companion object {
        private const val TOPIC = "jd.preprocess.text.request"
    }

    fun publish(message: JdPreprocessRequestMessage) {
        kafkaTemplate.send(
            TOPIC,
            message.eventId, // key → eventId 기준 파티셔닝
            message
        )
    }
}
