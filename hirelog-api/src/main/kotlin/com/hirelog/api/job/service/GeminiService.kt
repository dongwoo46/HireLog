package com.hirelog.api.job.service

import com.hirelog.api.config.properties.GeminiProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@Service
class GeminiService(
    @Qualifier("geminiWebClient")
    private val webClient: WebClient,
    private val geminiProperties: GeminiProperties
) {

    /**
     * 단순 텍스트 → Gemini 응답 텍스트 반환
     * (중복 체크 이후, 테스트/요약용)
     */
    fun analyzeJobDescription(jdText: String): String {
        val prompt = buildPrompt(jdText)

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

        return extractText(response)
    }


    /**
     * Gemini 응답 파싱
     * candidates[0].content.parts[0].text
     */
    private fun extractText(response: Map<*, *>?): String {
        if (response == null) return ""

        val candidates = response["candidates"] as? List<*>
        val firstCandidate = candidates?.firstOrNull() as? Map<*, *>
        val content = firstCandidate?.get("content") as? Map<*, *>
        val parts = content?.get("parts") as? List<*>
        val firstPart = parts?.firstOrNull() as? Map<*, *>

        return firstPart?.get("text") as? String ?: ""
    }

    private fun buildPrompt(jdText: String): String {
        return """
    너는 채용 공고(Job Description)를 분석하는 시스템이다.

    [규칙]
    - 반드시 JSON만 출력한다
    - 설명 문장, 마크다운, 코드블록 사용 금지
    - 값이 없으면 null로 표기
    - 추측하지 말고, JD에 명시된 내용만 사용

    [출력 포맷]
    {
      "company": string | null,
      "position": string | null,
      "date": string | null,
      "skills": [string],
      "requirements": [string],
      "summary": string
    }

    [JD 원문]
    $jdText
    """.trimIndent()
    }

}
