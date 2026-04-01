package com.hirelog.api.common.exception

import org.springframework.http.HttpStatus

enum class BusinessErrorCode(
    val status: HttpStatus,
    val code: String,
    val defaultMessage: String
) {
    INVALID_REVIEW_FILTER(
        status = HttpStatus.BAD_REQUEST,
        code = "INVALID_REVIEW_FILTER",
        defaultMessage = "리뷰 필터 조건이 올바르지 않습니다."
    ),
    INVALID_REQUEST_PARAMETER(
        status = HttpStatus.BAD_REQUEST,
        code = "INVALID_REQUEST_PARAMETER",
        defaultMessage = "요청 파라미터가 올바르지 않습니다."
    )
}
