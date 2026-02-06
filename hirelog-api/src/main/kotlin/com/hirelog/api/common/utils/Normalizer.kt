package com.hirelog.api.common.utils

object Normalizer {

    /**
     * 허용 문자:
     * - 영문 소문자
     * - 숫자
     * - 한글
     */
    private val ALLOWED_CHARS = Regex("[^a-z0-9가-힣]")
    private val SEPARATOR_CHARS = Regex("[^a-z0-9가-힣]+")

    /**
     * 법인 접미사 제거용 패턴
     *
     * - 한글/영문 혼합 고려
     */
    private val COMPANY_SUFFIXES = listOf(
        "주식회사",
        "(주)",
        "㈜",
        "유한회사",
        "inc",
        "corp",
        "corporation",
        "ltd",
        "co",
        "company"
    )

    fun normalizeBrand(value: String): String =
        value
            .trim()
            .lowercase()
            .replace(ALLOWED_CHARS, "")

    fun normalizePosition(value: String): String =
        value
            .trim()
            .lowercase()
            .replace(SEPARATOR_CHARS, "_")
            .replace(Regex("_+"), "_")
            .trim('_')

    /**
     * 회사(법인)명 정규화
     *
     * 목적:
     * - 법인 중복 판별
     * - 식별자 비교
     */
    fun normalizeCompany(value: String): String {
        var normalized = value.trim().lowercase()

        // 1️⃣ 법인 접미사 제거
        COMPANY_SUFFIXES.forEach { suffix ->
            normalized = normalized.replace(suffix, "")
        }

        // 2️⃣ 허용 문자만 남김 (붙은 문자열)
        normalized = normalized.replace(ALLOWED_CHARS, "")

        return normalized
    }

    fun normalizePositionCategory(value: String): String =
        value
            .trim()
            .lowercase()
            .replace(SEPARATOR_CHARS, "_")
            .replace(Regex("_+"), "_")
            .trim('_')
}
