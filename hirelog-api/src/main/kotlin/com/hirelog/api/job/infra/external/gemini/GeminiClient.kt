package com.hirelog.api.job.infrastructure.external.gemini

import com.hirelog.api.config.properties.GeminiProperties
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

/**
 * Gemini API HTTP Client
 *
 * 책임:
 * - Gemini REST API 호출
 * - 인증 파라미터(apiKey) 적용
 * - 요청/응답 포맷 구성
 * - 네트워크 타임아웃 관리
 *
 * 설계 원칙:
 * - Gemini API 사용법을 캡슐화한다
 * - WebClient 생성/설정은 외부(Config)에서 주입받는다
 * - 비즈니스 로직 또는 도메인 로직을 포함하지 않는다
 */
class GeminiClient(
    private val webClient: WebClient,
    private val geminiProperties: GeminiProperties
) {

    /**
     * Gemini API를 호출하여 프롬프트에 대한 응답 텍스트를 생성한다.
     *
     * 처리 흐름:
     * 1. Gemini API 요구 스펙에 맞는 request body 구성
     * 2. 모델명 및 API Key를 포함한 POST 요청 전송
     * 3. 네트워크 타임아웃 적용
     * 4. 응답 JSON에서 최종 텍스트 추출
     *
     * 주의:
     * - 이 메서드는 blocking 방식으로 동작한다
     * - 반드시 트랜잭션 외부에서 호출되어야 한다
     *
     * @param prompt Gemini에 전달할 입력 프롬프트
     * @return Gemini가 생성한 응답 텍스트
     */
    fun generateContent(prompt: String): String {

        // Gemini API 요청 스펙에 맞춘 request body 구성
        val requestBody = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to prompt)
                    )
                )
            )
        )

        // Gemini API 호출 및 응답 수신
        val response = webClient.post()
            .uri {
                it.path("/models/{model}:generateContent")
                    // Gemini API 인증용 API Key
                    .queryParam("key", geminiProperties.apiKey)
                    // 호출할 Gemini 모델명 바인딩
                    .build(geminiProperties.model)
            }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            // Gemini 응답을 Map 형태로 수신 (구조적 파싱은 별도 처리)
            .bodyToMono(Map::class.java)
            // 네트워크 지연에 대비한 타임아웃 설정
            .timeout(Duration.ofSeconds(30))
            // 현재 구조에서는 동기 처리
            .block()

        // 응답 JSON에서 최종 텍스트 추출
        return extractText(response)
    }

    /**
     * Gemini API 응답 JSON에서 생성된 텍스트를 추출한다.
     *
     * 응답 구조:
     * candidates[0].content.parts[0].text
     *
     * 설계 의도:
     * - Gemini 응답 포맷 변경 시 수정 지점을 한 곳으로 집중
     * - null/구조 오류 발생 시 즉시 실패
     *
     * @param response Gemini API 응답 전체 객체
     * @return 생성된 텍스트
     * @throws IllegalStateException 응답이 없거나 구조가 예상과 다를 경우
     */
    private fun extractText(response: Map<*, *>?): String {

        // Gemini API가 응답을 반환하지 않은 경우
        if (response == null) {
            throw IllegalStateException("Gemini API 응답이 null입니다")
        }

        // Gemini 응답 구조 단계별 접근
        val candidates = response["candidates"] as? List<*>
        val firstCandidate = candidates?.firstOrNull() as? Map<*, *>
        val content = firstCandidate?.get("content") as? Map<*, *>
        val parts = content?.get("parts") as? List<*>
        val firstPart = parts?.firstOrNull() as? Map<*, *>

        // 최종 텍스트 추출
        return firstPart?.get("text") as? String
            ?: throw IllegalStateException("Gemini 응답에서 텍스트를 추출할 수 없습니다")
    }
}
