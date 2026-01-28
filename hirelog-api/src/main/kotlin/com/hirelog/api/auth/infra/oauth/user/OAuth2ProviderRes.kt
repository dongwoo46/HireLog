package com.hirelog.api.auth.infra.oauth.user

import com.hirelog.api.auth.domain.OAuth2Provider

/**
 * OAuth Provider 응답 공통 인터페이스
 *
 * 책임:
 * - Provider별 응답(JSON)을 내부 표준 필드로 정규화
 */
interface OAuth2ProviderRes {

    fun getProvider(): OAuth2Provider

    /** OAuth Provider가 발급한 사용자 고유 ID */
    fun getProviderUserId(): String

    /** 사용자 이메일 (없을 수 있음) */
    fun getEmail(): String?

}
