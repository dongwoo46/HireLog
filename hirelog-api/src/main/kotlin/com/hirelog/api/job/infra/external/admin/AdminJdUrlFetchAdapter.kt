package com.hirelog.api.job.infra.external.admin

import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.port.AdminJdUrlFetchPort
import com.hirelog.api.job.application.summary.port.AdminJdUrlFetchPort.AdminJdFetchResult
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

/**
 * Admin URL JD 추출 어댑터
 *
 * Python 임베딩 서버 /admin/fetch-url 동기 호출.
 * Playwright 크롤링을 포함하므로 timeout을 90초로 설정한다.
 */
@Component
class AdminJdUrlFetchAdapter(
    @Qualifier("embeddingWebClient") private val webClient: WebClient
) : AdminJdUrlFetchPort {

    companion object {
        private val TIMEOUT = Duration.ofSeconds(90)
    }

    override fun fetch(url: String): AdminJdFetchResult {
        log.info("[ADMIN_JD_FETCH] url={}", url)

        val response = webClient.post()
            .uri("/admin/fetch-url")
            .bodyValue(mapOf("url" to url))
            .retrieve()
            .bodyToMono(AdminFetchUrlHttpResponse::class.java)
            .timeout(TIMEOUT)
            .block()
            ?: return AdminJdFetchResult.Error("임베딩 서버 응답이 null입니다")

        return when (response.status) {
            "SUCCESS" -> {
                val text = response.text
                    ?: return AdminJdFetchResult.Error("SUCCESS 상태이지만 text가 null입니다")
                AdminJdFetchResult.Success(text)
            }
            "IMAGE_BASED" -> {
                val images = response.images
                    ?: return AdminJdFetchResult.Error("IMAGE_BASED 상태이지만 images가 null입니다")
                AdminJdFetchResult.ImageBased(images = images, partialText = response.text)
            }
            "INSUFFICIENT" -> AdminJdFetchResult.Insufficient(response.message ?: "텍스트 추출 불충분")
            "ERROR" -> AdminJdFetchResult.Error(response.message ?: "알 수 없는 오류")
            else -> AdminJdFetchResult.Error("알 수 없는 status: ${response.status}")
        }
    }

    data class AdminFetchUrlHttpResponse(
        val status: String,
        val text: String? = null,
        val images: List<String>? = null,
        val message: String? = null
    )
}