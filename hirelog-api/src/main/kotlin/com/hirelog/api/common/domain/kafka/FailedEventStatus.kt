package com.hirelog.api.common.domain.kafka

enum class FailedEventStatus {
    FAILED,      // 초기 상태 (DLT 전송됨)
    REPROCESSED, // 재처리 완료
    IGNORED      // 수동으로 무시 처리
}