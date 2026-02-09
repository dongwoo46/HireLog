package com.hirelog.api.job.infra.external.openai

import com.hirelog.api.common.config.properties.LlmProviderProperties
import com.hirelog.api.job.infrastructure.external.gemini.GeminiPromptBuilder
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import java.util.concurrent.CompletableFuture

/**
 * OpenAI Chat Completions API HTTP Client
 *
 * 책임:
 * - OpenAI API 비동기 호출
 * - 응답에서 텍스트 추출
 *
 * 설계 원칙:
 * - Gemini와 동일한 프롬프트 사용 (GeminiPromptBuilder 재사용)
 * - JSON 응답 강제 (response_format)
 */
class OpenAiClient(
    private val webClient: WebClient,
    private val properties: LlmProviderProperties
) {

    fun generateContentAsync(prompt: String): CompletableFuture<String> {

        val requestBody: Map<String, Any> = mapOf(
            "model" to properties.model,
            "temperature" to 0.2,
            "response_format" to mapOf("type" to "json_object"),
            "messages" to listOf(
                mapOf(
                    "role" to "system",
                    "content" to GeminiPromptBuilder.buildSystemInstruction()
                ),
                mapOf(
                    "role" to "user",
                    "content" to prompt
                )
            )
        )

        return webClient.post()
            .uri("/chat/completions")
            .header("Authorization", "Bearer ${properties.apiKey}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map::class.java)
            .timeout(Duration.ofSeconds(properties.timeoutMs / 1000))
            .map { response -> extractText(response) }
            .toFuture()
    }

    /**
     * OpenAI 응답에서 텍스트 추출
     *
     * 응답 구조:
     * choices[0].message.content
     */
    private fun extractText(response: Map<*, *>?): String {
        if (response == null) {
            throw IllegalStateException("OpenAI API 응답이 null입니다")
        }

        val choices = response["choices"] as? List<*>
        val firstChoice = choices?.firstOrNull() as? Map<*, *>
        val message = firstChoice?.get("message") as? Map<*, *>

        return message?.get("content") as? String
            ?: throw IllegalStateException("OpenAI 응답에서 텍스트를 추출할 수 없습니다")
    }
}
