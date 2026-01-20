package com.hirelog.api.jd.application.messaging

/**
 * JD 비동기 처리용 Stream Key 정의
 *
 * 책임:
 * - JD 파이프라인의 논리적 채널 이름을 정의
 *
 * 비책임:
 * - Redis / Kafka 기술 의존 ❌
 */
object JdStreamKeys {

    /**
     * JD 전처리 요청 Stream
     *
     * 용도:
     * - 원문 JD 전처리
     * - canonical hash 생성
     * - 중복 판단
     */
    const val PREPROCESS = "jd:preprocess:stream"

    /**
     * JD OCR 처리 요청 Stream
     *
     * 용도:
     * - 이미지 기반 JD의 OCR 처리
     */
    const val OCR = "jd:ocr:stream"

}
