package com.hirelog.api.auth.infra.oauth.user

import com.hirelog.api.auth.domain.OAuth2Provider

class GoogleRes(
    private val attributes: Map<String, Any>
) : OAuth2ProviderRes {

    override fun getProvider(): OAuth2Provider = OAuth2Provider.GOOGLE

    override fun getProviderUserId(): String =
        attributes["sub"] as String

    override fun getEmail(): String? =
        attributes["email"] as? String

}
