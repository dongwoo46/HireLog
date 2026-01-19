package com.hirelog.api.common.utils

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object Hasher {

    /**
     * SHA-256 기반 텍스트 해시
     *
     * 설계 원칙:
     * - 플랫폼 독립성 보장
     * - 동일 입력 → 항상 동일 해시
     * - JD 중복 판별의 기준 값
     */
    fun hash(rawText: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(rawText.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
