package com.hirelog.api.auth.infra.oauth.handler

import com.hirelog.api.auth.domain.OAuth2LoginResult
import com.hirelog.api.auth.domain.OAuthUser

/**
 * OAuth 로그인 처리 포트
 *
 * 책임:
 * - OAuthUser를 받아 기존 회원 여부를 판별하고 OAuth2LoginResult를 반환
 */
interface OAuth2LoginProcessor {
    fun loginProcess(oAuthUser: OAuthUser): OAuth2LoginResult
}
