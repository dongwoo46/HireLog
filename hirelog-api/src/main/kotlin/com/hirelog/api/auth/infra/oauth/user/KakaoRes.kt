package com.hirelog.api.auth.infra.oauth.user

import com.hirelog.api.auth.domain.OAuth2Provider

class KakaoRes(
    private val attributes: Map<String, Any>
) : OAuth2ProviderRes {

    override fun getProvider(): OAuth2Provider = OAuth2Provider.KAKAO

    override fun getProviderUserId(): String =
        attributes["id"].toString()

    override fun getEmail(): String? {
        val kakaoAccount = attributes["kakao_account"] as? Map<*, *> ?: return null
        return kakaoAccount["email"] as? String
    }

}
