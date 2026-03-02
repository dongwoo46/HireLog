package com.hirelog.api.common.exception

/**
 * 잘못된 cursor 전달 시 발생
 *
 * 케이스:
 * - Base64 디코딩 실패
 * - JSON 역직렬화 실패 (unknown type, 필드 누락)
 * - sortBy와 cursor 타입 불일치
 *
 * HTTP: 400 Bad Request
 */
class InvalidCursorException(reason: String) : RuntimeException(reason)
