package com.hirelog.api.relation.domain.type

enum class MemberJobSummarySaveType {

    /**
     * 즐겨찾기
     */
    FAVORITE,

    /**
     * 지원 예정
     */
    APPLY,

    /**
     * 다른 JD와 비교용
     */
    COMPARE,

    /**
     * 아카이브 (나중에 다시 보기)
     */
    ARCHIVE
}
