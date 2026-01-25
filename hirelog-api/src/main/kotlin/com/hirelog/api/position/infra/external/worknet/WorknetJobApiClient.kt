package com.hirelog.api.position.infra.external.worknet

import com.hirelog.api.common.config.properties.WorknetApiProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

/**
 * WorknetJobApiClient
 *
 * 책임:
 * - 워크넷 OPEN API 호출 전용 Client
 * - 직업 목록 / 상세 조회
 *
 * 주의:
 * - 비즈니스 판단 ❌
 * - raw 데이터 가공 ❌
 * - 외부 통신만 담당
 */
@Component
class WorknetJobApiClient(
    private val props: WorknetApiProperties,

    @Qualifier("worknetWebClient")
    private val webClient: WebClient
) {

    /**
     * 워크넷 직업 목록 RAW 조회
     *
     * 반환:
     * - XML String (파싱 책임은 Translator)
     */
    fun fetchJobListRaw(): String {
        try {
            return webClient.get()
                .uri {
                    it
                        .queryParam("authKey", props.key)
                        .queryParam("returnType", "XML")
                        .queryParam("target", "JOBCD")
                        .build()
                }
                .retrieve()
                .bodyToMono(String::class.java)
                .block()
                ?: throw IllegalStateException("Worknet API returned empty body")
        } catch (e: WebClientResponseException) {
            throw IllegalStateException(
                "Worknet API error: status=${e.statusCode}, body=${e.responseBodyAsString}",
                e
            )
        } catch (e: Exception) {
            throw IllegalStateException(
                "Failed to call Worknet job list API",
                e
            )
        }
    }
}
