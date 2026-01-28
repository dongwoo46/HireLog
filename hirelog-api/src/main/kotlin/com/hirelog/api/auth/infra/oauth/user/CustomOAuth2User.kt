package com.hirelog.api.auth.infra.oauth.user

import com.hirelog.api.auth.domain.OAuthUser
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

class CustomOAuth2User(
    val oAuthUser: OAuthUser,
    private val attributes: Map<String, Any>,
) : OAuth2User {

    override fun getAttributes(): Map<String, Any> = attributes

    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()

    override fun getName(): String =
        "${oAuthUser.provider.name}_${oAuthUser.providerUserId}"

}
