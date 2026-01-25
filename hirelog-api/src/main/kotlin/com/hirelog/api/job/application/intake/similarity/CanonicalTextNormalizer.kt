package com.hirelog.api.job.intake.similarity

import com.hirelog.api.job.application.intake.model.JdSection

/**
 * CanonicalMap을 결정론적 텍스트로 변환
 *
 * 용도:
 * - 디버깅
 * - 로그
 * - fallback similarity
 */
object CanonicalTextNormalizer {

    fun toCanonicalText(
        canonicalMap: Map<String, List<String>>
    ): String {
        return JdSection.entries
            .mapNotNull { section ->
                canonicalMap[section.key]
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { lines ->
                        formatSection(section, lines)
                    }
            }
            .joinToString("\n\n")
            .trim()
    }

    private fun formatSection(
        section: JdSection,
        lines: List<String>
    ): String =
        buildString {
            appendLine(section.name)
            lines.forEach(::appendLine)
        }.trimEnd()
}
