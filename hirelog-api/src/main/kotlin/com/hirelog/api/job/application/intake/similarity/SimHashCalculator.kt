package com.hirelog.api.job.intake.similarity

import com.hirelog.api.job.application.intake.model.JdSection
import java.security.MessageDigest

/**
 * SimHash 계산기
 *
 * 최적화 포인트:
 * - MessageDigest ThreadLocal 재사용
 * - token → weight 기반 계산
 */
object SimHashCalculator {

    /**
     * ThreadLocal MD5 인스턴스
     *
     * 이유:
     * - MessageDigest는 thread-safe 하지 않음
     * - 반복 생성 비용 제거
     */
    private val md5ThreadLocal: ThreadLocal<MessageDigest> =
        ThreadLocal.withInitial {
            MessageDigest.getInstance("MD5")
        }

    fun calculate(
        canonicalMap: Map<String, List<String>>
    ): Long {

        val tokenWeights = buildTokenWeights(canonicalMap)
        if (tokenWeights.isEmpty()) return 0L

        return calculateSimHash(tokenWeights)
    }

    /**
     * 섹션 가중치를 반영한 token → weight 맵 생성
     */
    private fun buildTokenWeights(
        canonicalMap: Map<String, List<String>>
    ): Map<String, Int> {

        val weights = mutableMapOf<String, Int>()

        JdSection.entries.forEach { section ->
            val lines = canonicalMap[section.key].orEmpty()
            lines.forEach { line ->
                SimHashTokenizer.tokenize(line).forEach { token ->
                    weights[token] =
                        weights.getOrDefault(token, 0) + section.weight
                }
            }
        }

        return weights
    }

    /**
     * SimHash 핵심 알고리즘
     */
    private fun calculateSimHash(
        tokenWeights: Map<String, Int>
    ): Long {

        val bitVector = IntArray(64)

        tokenWeights.forEach { (token, weight) ->
            val hash = token.hash64()
            for (i in 0 until 64) {
                if ((hash ushr i) and 1L == 1L) {
                    bitVector[i] += weight
                } else {
                    bitVector[i] -= weight
                }
            }
        }

        var result = 0L
        for (i in 0 until 64) {
            if (bitVector[i] > 0) {
                result = result or (1L shl i)
            }
        }

        return result
    }

    /**
     * 안정적인 64-bit 해시
     *
     * 구현:
     * - MD5 128bit 중 하위 64bit 사용
     * - ThreadLocal MessageDigest 재사용
     */
    private fun String.hash64(): Long {
        val md5 = md5ThreadLocal.get()
        md5.reset()

        val digest = md5.digest(toByteArray(Charsets.UTF_8))

        var hash = 0L
        for (i in 0 until 8) {
            hash = hash or ((digest[i].toLong() and 0xff) shl (i * 8))
        }
        return hash
    }
}
