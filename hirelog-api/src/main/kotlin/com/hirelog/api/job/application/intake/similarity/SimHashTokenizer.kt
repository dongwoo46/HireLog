package com.hirelog.api.job.intake.similarity

/**
 * SimHash 전용 토크나이저
 *
 * 정책:
 * - 소문자 통일
 * - 문자 포함 필수 (숫자-only 제거)
 * - 길이 >= 2
 * - 특수문자 제거
 */
object SimHashTokenizer {

    private val SPLIT_REGEX = Regex("[\\s+\\-_/]+")

    fun tokenize(text: String): List<String> {
        return text
            .lowercase()
            .split(SPLIT_REGEX)
            .asSequence()
            .map { it.trim { ch -> !ch.isLetterOrDigit() } }
            .filter { it.length >= 2 }
            .filter { it.any(Char::isLetter) }
            .toList()
    }
}
