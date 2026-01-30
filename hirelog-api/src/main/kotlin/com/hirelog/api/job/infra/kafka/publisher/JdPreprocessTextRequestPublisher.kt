package com.hirelog.api.job.infra.kafka.publisher

import com.hirelog.api.job.application.messaging.JdPreprocessRequestMessage
import com.hirelog.api.job.infra.kafka.topic.JdKafkaTopics
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
        private const val TOPIC = JdKafkaTopics.PREPROCESS_TEXT_REQUEST
    }

    fun publish(message: JdPreprocessRequestMessage) {
        val future = kafkaTemplate.send(TOPIC, message.eventId, message)

        // 전송 결과를 기다리고 로그를 찍어봅니다.
        future.whenComplete { result, ex ->
            if (ex == null) {
                println("✅ [Kafka] 전송 성공: topic=${result.recordMetadata.topic()}, partition=${result.recordMetadata.partition()}, offset=${result.recordMetadata.offset()}")
            } else {
                println("❌ [Kafka] 전송 실패: ${ex.message}")
            }
        }
    }
}
