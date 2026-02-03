package com.hirelog.api.auth.application

import com.hirelog.api.auth.domain.OAuth2LoginResult
import com.hirelog.api.auth.domain.OAuthUser
import com.hirelog.api.auth.infra.oauth.handler.OAuth2LoginProcessor
import com.hirelog.api.member.application.port.MemberCommand
import com.hirelog.api.member.application.port.MemberOAuthAccountQuery
import com.hirelog.api.member.application.port.MemberQuery
import com.hirelog.api.member.domain.Member
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * OAuth 로그인 처리 서비스
 *
 * 책임:
 * - OAuthUser → 기존 회원 / 신규 회원 판별
 * - 기존 회원: memberId 반환
 * - 신규 회원: Member 생성 + OAuth 계정 연결 후 반환
 *
 * 정책:
 * - provider + providerUserId 조합으로 기존 연결 여부를 먼저 확인
 * - 연결이 없으면 email 기준으로 기존 Member를 찾아 OAuth 계정을 연결
 * - email도 없으면 신규 Member 생성
 */
@Service
class OAuthLoginService(
    private val memberOAuthAccountQuery: MemberOAuthAccountQuery,
) : OAuth2LoginProcessor {

    @Transactional(readOnly = true) // 단순 조회만 수행
    override fun loginProcess(oAuthUser: OAuthUser): OAuth2LoginResult {

        // 1. 소셜 고유 식별자로 기존 연결 확인
        val existingAccount = memberOAuthAccountQuery.findByProviderAndProviderUserId(
            provider = oAuthUser.provider,
            providerUserId = oAuthUser.providerUserId,
        )

        // 가입된 적이 있다면 바로 기존 유저 리턴
        if (existingAccount != null) {
            return OAuth2LoginResult.ExistingUser(existingAccount.member)
        }

        // 2. 가입된 적이 없다면? DB에 저장하지 않고 정보를 담아 NewUser 리턴
        // 이 결과를 받은 SuccessHandler가 프론트엔드의 가입 페이지로 리다이렉트 시킵니다.
        return OAuth2LoginResult.NewUser(
            email = oAuthUser.email, // 소셜에서 준 이메일 (null일 수 있음)
            provider = oAuthUser.provider,
            providerUserId = oAuthUser.providerUserId
        )
    }
}