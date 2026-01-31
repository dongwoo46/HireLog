package com.hirelog.api.userrequest.domain

enum class UserRequestType {
    MODIFY_REQUEST,     // 내용 수정 요청
    ERROR_REPORT,       // 오류/잘못된 데이터 신고
    FEATURE_REQUEST,    // 기능 요청
    REPROCESS_REQUEST   // 재처리 요청 (LLM, 파이프라인)
}
