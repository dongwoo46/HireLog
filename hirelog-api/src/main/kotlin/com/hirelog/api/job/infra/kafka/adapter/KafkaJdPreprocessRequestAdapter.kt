package com.hirelog.api.job.infra.kafka.adapter

import com.hirelog.api.job.application.intake.port.JdPreprocessRequestPort
import com.hirelog.api.job.application.messaging.JdPreprocessRequestMessage
import com.hirelog.api.job.domain.type.JobSourceType
import com.hirelog.api.job.infra.kafka.publisher.JdPreprocessOcrRequestPublisher
import com.hirelog.api.job.infra.kafka.publisher.JdPreprocessTextRequestPublisher
import com.hirelog.api.job.infra.kafka.publisher.JdPreprocessUrlRequestPublisher
import org.springframework.stereotype.Component

/**
 * Kafka 기반 JD 전처리 요청 Adapter
 *
 * 책임:
 * - source 기준 Kafka topic / publisher 선택
 */
@Component
class KafkaJdPreprocessRequestAdapter(
    private val textPublisher: JdPreprocessTextRequestPublisher,
    private val ocrPublisher: JdPreprocessOcrRequestPublisher,
    private val urlPublisher: JdPreprocessUrlRequestPublisher,
) : JdPreprocessRequestPort {

    override fun send(request: JdPreprocessRequestMessage) {
        when (request.source) {
            JobSourceType.TEXT ->
                textPublisher.publish(request)

            JobSourceType.IMAGE ->
                ocrPublisher.publish(request)

            JobSourceType.URL ->
                urlPublisher.publish(request)
        }
    }
}
