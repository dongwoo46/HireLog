package com.hirelog.api.job.infrastructure.external.gemini

/**
 * Gemini JD 요약 프롬프트 빌더
 *
 * 책임:
 * - JD 요약 요구사항을 프롬프트로 변환
 * - 프롬프트 정책 변경 시 영향 범위 최소화
 */
object GeminiPromptBuilder {

    fun buildJobSummaryPrompt(
        brandName: String,
        position: String,
        jdText: String
    ): String {
        return """
            You are an AI system that analyzes a Job Description (JD) and produces a structured summary.

            [Rules]
            - Output MUST be valid JSON only
            - Do NOT include explanations, markdown, or code blocks
            - Do NOT infer or guess missing information
            - Use ONLY information explicitly stated in the JD
            - If a value does not exist, use null
            - All text values MUST be written in Korean

            [Fixed Input]
            - brandName: $brandName
            - position: $position

            [Output JSON Format]
            {
              "careerType": "ENTRY" | "EXPERIENCED" | "ANY",
              "careerYears": number | null,
              "summary": string,
              "responsibilities": string,
              "requiredQualifications": string,
              "preferredQualifications": string | null,
              "techStack": string | null,
              "recruitmentProcess": string | null
            }

            [Job Description]
            $jdText
        """.trimIndent()
    }
}
