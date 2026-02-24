package com.hirelog.api.common.infra.kafka

import com.hirelog.api.common.logging.log
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
            .whenComplete { result, ex ->
                if (ex != null) {
                    log.error(
                        "[KAFKA_PUBLISH_FAILED] topic={}, key={}, error={}",
                        topic, key, ex.message, ex
                    )
                } else {
                    log.debug(
                        "[KAFKA_PUBLISH_SUCCESS] topic={}, key={}, partition={}, offset={}",
                        topic, key,
                        result.recordMetadata.partition(),
                        result.recordMetadata.offset()
                    )
                }
            }
    }
}
