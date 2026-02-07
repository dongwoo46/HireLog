package com.hirelog.api.relation.domain.type

enum class MemberJobSummarySaveType {

    /**
     * 단순 저장 (기본값)
     */
    SAVED,

    /**
     * 지원 의사가 있는 JD
     */
    APPLY,

    // 저장해제
    UNSAVED
}

