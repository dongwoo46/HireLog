package com.hirelog.api.job.application.summary.port

/**
 * Admin URL JD 추출 포트
 *
 * Python 임베딩 서버(/admin/fetch-url)를 호출하여
 * URL에서 JD 텍스트 또는 이미지를 추출한다.
 */
interface AdminJdUrlFetchPort {

    fun fetch(url: String): AdminJdFetchResult

    sealed class AdminJdFetchResult {
        /** 텍스트 추출 성공 */
        data class Success(val text: String) : AdminJdFetchResult()

        /**
         * 이미지 기반 JD
         * images: "data:{mime};base64,{data}" 형식 리스트
         */
        data class ImageBased(
            val images: List<String>,
            val partialText: String?
        ) : AdminJdFetchResult()

        /** 텍스트/이미지 모두 불충분 — admin이 직접 텍스트 입력 필요 */
        data class Insufficient(val message: String) : AdminJdFetchResult()

        /** 크롤링/파싱 실패 */
        data class Error(val message: String) : AdminJdFetchResult()
    }
}
