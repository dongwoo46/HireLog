package com.hirelog.api.job.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.hirelog.api.common.exception.GeminiParseException
import com.hirelog.api.common.logging.log
import com.hirelog.api.config.properties.GeminiProperties
import com.hirelog.api.job.dto.JobSummaryResult
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@Service
class GeminiService(
    @Qualifier("geminiWebClient")
    private val webClient: WebClient,
    private val geminiProperties: GeminiProperties,
    private val objectMapper: ObjectMapper
) {

    /**
     * 단순 텍스트 → Gemini 응답 텍스트 반환
     * (중복 체크 이후, 테스트/요약용)
     */
    fun summaryTextJobDescription(
        brandName: String,
        position: String,
        jdText: String
    ): GeminiSummary {
        val prompt = buildJobSummaryPrompt(brandName, position, jdText)

        val requestBody = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to prompt)
                    )
                )
            )
        )

        val response = webClient.post()
            .uri {
                it.path("/models/{model}:generateContent")  // 전체 경로 명시
                    .queryParam("key", geminiProperties.apiKey)
                    .build(geminiProperties.model)
            }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map::class.java)
            .timeout(Duration.ofSeconds(30))
            .block()

        val rawText = extractText(response)
        val normalizedJson = normalizeGeminiJson(rawText)

        try {
            return objectMapper.readValue(
                normalizedJson,
                GeminiSummary::class.java
            )
        } catch (e: JsonProcessingException) {
            log.error(
                "Failed to parse Gemini response. length={}, snippet={}",
                normalizedJson.length,
                normalizedJson.take(300),
                e
            )
            throw GeminiParseException(e)
        }
    }


    /**
     * Gemini 응답 파싱
     * candidates[0].content.parts[0].text
     */
    private fun extractText(response: Map<*, *>?): String {
        if (response == null) {
            throw IllegalStateException("Gemini API 응답이 null입니다")
        }

        val candidates = response["candidates"] as? List<*>
        val firstCandidate = candidates?.firstOrNull() as? Map<*, *>
        val content = firstCandidate?.get("content") as? Map<*, *>
        val parts = content?.get("parts") as? List<*>
        val firstPart = parts?.firstOrNull() as? Map<*, *>

        return firstPart?.get("text") as? String ?: throw IllegalStateException("Gemini 응답에서 텍스트를 추출할 수 없습니다")
    }

    private fun normalizeGeminiJson(raw: String): String {
        return raw
            .replace(Regex("```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace("```", "")
            .trim()
    }


    private fun buildJobSummaryPrompt(
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
            - The brand name and position provided below are fixed inputs
            - Do NOT modify, reinterpret, or regenerate brand name or position
    
            [Fixed Input]
            - brandName: $brandName
            - position: $position
    
            [Output JSON Format]
            {
              "brandName": string,
              "position": string,
    
              "careerType": "ENTRY" | "EXPERIENCED" | "ANY",
              "careerYears": number | null,
    
              "summary": string,
              "responsibilities": string,
              "requiredQualifications": string,
              "preferredQualifications": string | null,
              "techStack": string | null,
              "recruitmentProcess": string | null
            }
    
            [Career Rules]
            - careerType:
              - ENTRY: 신입만 명시된 경우
              - EXPERIENCED: 경력만 명시된 경우
              - ANY: 신입/경력 무관, 또는 둘 다 가능한 경우
            - careerYears:
              - Extract only the minimum required years if explicitly stated
              - Example: "3년 이상" → 3
              - If not clearly stated, return null
            - Do NOT guess career information
    
            [Guidelines]
            - "brandName" MUST exactly match the fixed input
            - "position" MUST exactly match the fixed input
            - "summary": 3–5 sentences summarizing the overall role and purpose
            - "responsibilities": core duties and responsibilities of the position
            - "requiredQualifications": mandatory requirements or qualifications
            - "preferredQualifications": preferred or optional qualifications
            - "techStack": technologies, frameworks, or tools mentioned
            - "recruitmentProcess": hiring steps if explicitly mentioned
    
            [Job Description]
            $jdText
        """.trimIndent()
    }


}
