package com.hirelog.api.auth.infra.oauth.user

import com.hirelog.api.auth.domain.OAuth2Provider
import com.hirelog.api.auth.domain.OAuthUser
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component

/**
 * CustomOAuth2UserService
 *
 * 책임:
 * - Spring Security OAuth2 로그인 후 Provider 응답을 CustomOAuth2User로 변환
 * - Provider별 응답 차이를 OAuth2ProviderRes 구현체로 정규화
 */
/**
 * CustomOAuth2UserService
 *
 * 역할:
 * - OAuth Provider에서 내려준 사용자 정보를 수신
 * - Provider별 응답을 내부 표준(OAuthUser)으로 변환
 *
 * 주의:
 * - 회원 조회 / 생성 / 연결 로직은 절대 포함하지 않음
 */
@Component
class CustomOAuth2UserService : DefaultOAuth2UserService() {

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        // Provider API 호출
        val oAuth2User = super.loadUser(userRequest)

        // provider 식별 (google, kakao 등)
        val registrationId = userRequest.clientRegistration.registrationId

        // Provider 응답 정규화
        val providerRes = createProviderRes(registrationId, oAuth2User.attributes)

        // 내부 표준 OAuthUser 생성
        val oAuthUser = OAuthUser(
            provider = providerRes.getProvider(),
            providerUserId = providerRes.getProviderUserId(),
            email = providerRes.getEmail(),
        )

        // Security Context에 저장될 CustomOAuth2User 반환
        return CustomOAuth2User(
            oAuthUser = oAuthUser,
            attributes = oAuth2User.attributes,
        )
    }

    /**
     * OAuth Provider별 응답 객체 생성
     */
    private fun createProviderRes(
        registrationId: String,
        attributes: Map<String, Any>,
    ): OAuth2ProviderRes {
        return when (registrationId.lowercase()) {
            "google" -> GoogleRes(attributes)
            "kakao" -> KakaoRes(attributes)
            else -> throw IllegalArgumentException(
                "지원하지 않는 OAuth Provider 입니다. registrationId=$registrationId"
            )
        }
    }


}