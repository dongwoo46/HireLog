package com.hirelog.api.company.domain

enum class CompanyCandidateStatus {
    /**
     * 검토 대기
     */
    PENDING,

    /**
     * 승인됨
     */
    APPROVED,

    /**
     * 거절됨
     */
    REJECTED,

    /**
     * Company 생성 완료
     */
    COMPLETED,

    /**
     * Company 처리중
     */
    PROCESSING,

    /**
     *  처리 실패 (재시도 대상)
     */
    FAILED

}
