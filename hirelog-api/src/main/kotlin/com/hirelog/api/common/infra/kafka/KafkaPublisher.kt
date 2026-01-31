package com.hirelog.api.common.infra.kafka

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * KafkaPublisher
 *
 * 책임:
 * - Kafka Topic으로 메시지 발행
 * - 전송 성공/실패 신호 전달
 */
@Component
class KafkaPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    /**
     * 메시지 발행
     *
     * @param topic Kafka Topic
     * @param key   메시지 Key (파티셔닝 기준)
     * @param value 전송할 Payload
     */
    fun publish(
        topic: String,
        key: String,
        value: Any
    ) {
        kafkaTemplate.send(topic, key, value)
    }
}
