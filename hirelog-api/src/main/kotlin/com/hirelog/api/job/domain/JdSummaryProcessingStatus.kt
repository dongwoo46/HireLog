package com.hirelog.api.job.domain

enum class JdSummaryProcessingStatus {

    /**
     * 전처리 결과 수신됨
     */
    RECEIVED,

    /**
     * 요약 요청 진행 중
     */
    SUMMARIZING,

    /**
     * 중복 JD로 처리 종료
     */
    DUPLICATE,

    /**
     * 요약 완료
     */
    COMPLETED,

    /**
     * 처리 실패
     */
    FAILED
}
