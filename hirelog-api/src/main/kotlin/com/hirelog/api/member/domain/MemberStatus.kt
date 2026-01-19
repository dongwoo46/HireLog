package com.hirelog.api.member.domain

enum class MemberStatus {
    ACTIVE,     // 정상
    SUSPENDED,  // 일시 정지
    DELETED     // 탈퇴 (soft delete)
}
