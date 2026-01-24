package com.hirelog.api.job.intake.similarity

/**
 * SimHash 유사도 판단
 */
object SimHashSimilarity {

    const val THRESHOLD_ALMOST_IDENTICAL = 5
    const val THRESHOLD_VERY_SIMILAR = 10
    const val THRESHOLD_SIMILAR = 15

    enum class SimilarityLevel {
        IDENTICAL,
        ALMOST_SAME,
        VERY_SIMILAR,
        SIMILAR,
        DIFFERENT
    }

    fun hammingDistance(a: Long, b: Long): Int {
        return (a xor b).countOneBits()
    }

    fun getSimilarityLevel(
        a: Long,
        b: Long
    ): SimilarityLevel {

        val distance = hammingDistance(a, b)

        return when {
            distance == 0 -> SimilarityLevel.IDENTICAL
            distance <= THRESHOLD_ALMOST_IDENTICAL -> SimilarityLevel.ALMOST_SAME
            distance <= THRESHOLD_VERY_SIMILAR -> SimilarityLevel.VERY_SIMILAR
            distance <= THRESHOLD_SIMILAR -> SimilarityLevel.SIMILAR
            else -> SimilarityLevel.DIFFERENT
        }
    }

    fun isDuplicate(
        a: Long,
        b: Long,
        threshold: Int = THRESHOLD_VERY_SIMILAR
    ): Boolean {
        return hammingDistance(a, b) <= threshold
    }
}
