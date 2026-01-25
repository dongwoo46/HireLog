package com.hirelog.api.jd.application.messaging

/**
 * JD 비동기 처리용 Stream Key 정의
 *
 * 책임:
 * - JD 파이프라인의 논리적 채널 이름 정의
 * - Spring ↔ Python 간 계약의 단일 기준점
 *
 * 설계 원칙:
 * - 요청 / 결과 분리
 * - TEXT / OCR 물리적 분리
 * - 워커 확장 및 장애 격리 가능
 *
 * 비책임:
 * - Redis / Kafka 등 기술 의존 ❌
 */
object JdStreamKeys {

    // ==================================================
    // PREPROCESS REQUEST (Spring → Python)
    // ==================================================

    /**
     * TEXT 기반 JD 전처리 요청
     *
     * 용도:
     * - 원문 TEXT JD 전처리
     * - canonical text 생성
     * - 구조화 / semantic-lite
     */
    const val PREPROCESS_TEXT_REQUEST =
        "jd:preprocess:text:request:stream"

    /**
     * OCR 기반 JD 전처리 요청
     *
     * 용도:
     * - 이미지 JD OCR 처리
     * - OCR 결과를 TEXT 전처리 파이프라인으로 연결
     */
    const val PREPROCESS_OCR_REQUEST =
        "jd:preprocess:ocr:request:stream"


    // ==================================================
    // PREPROCESS RESULT (Python → Spring)
    // ==================================================

    /**
     * TEXT,OCR,URL JD 전처리 결과
     */
    const val PREPROCESS_RESPONSE=
        "jd:preprocess:response:stream"


}
