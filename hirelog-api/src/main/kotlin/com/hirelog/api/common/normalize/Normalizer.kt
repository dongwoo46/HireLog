package com.hirelog.api.common.normalize

object Normalizer {

    private val BRAND_REGEX = Regex("[^a-z0-9가-힣]")

    fun normalizeBrand(value: String): String =
        value.lowercase()
            .replace(BRAND_REGEX, "")
            .trim()

    fun normalizePosition(value: String): String =
        value.lowercase()
            .replace(BRAND_REGEX, "")
            .trim()
}
