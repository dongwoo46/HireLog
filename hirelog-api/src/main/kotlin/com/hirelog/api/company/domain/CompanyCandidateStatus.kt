package com.hirelog.api.company.domain

enum class CompanyCandidateStatus {
    /**
     * 검토 대기
     */
    PENDING,

    /**
     * 승인됨 (Company 생성/연결 완료)
     */
    APPROVED,

    /**
     * 거절됨
     */
    REJECTED
}
