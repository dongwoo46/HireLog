package com.hirelog.api.common.utils

object Normalizer {

    private val BRAND_REGEX = Regex("[^a-z0-9가-힣]")

    fun normalizeBrand(value: String): String =
        value
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9가-힣]"), "")

    fun normalizePosition(value: String): String =
        value
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9가-힣]+"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
}
