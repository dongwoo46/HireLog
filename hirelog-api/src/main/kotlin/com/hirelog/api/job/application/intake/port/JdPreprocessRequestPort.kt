package com.hirelog.api.job.application.intake.port

import com.hirelog.api.job.application.messaging.JdPreprocessRequestMessage

/**
 * JD 전처리 요청 Port
 *
 * 책임:
 * - 전처리 요청 전송
 * - 전송 수단(Kafka 등) 추상화
 */
interface JdPreprocessRequestPort {

    fun send(request: JdPreprocessRequestMessage)
}
