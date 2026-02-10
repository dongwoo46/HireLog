package com.hirelog.api.common.config.security

import com.hirelog.api.auth.infra.jwt.JwtAuthenticationFilter
import com.hirelog.api.auth.infra.oauth.handler.OAuth2LoginSuccessHandler
import com.hirelog.api.auth.infra.oauth.repository.CookieOAuth2AuthorizationRequestRepository
import com.hirelog.api.auth.infra.oauth.user.CustomOAuth2UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val oAuth2LoginSuccessHandler: OAuth2LoginSuccessHandler,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    // 중간상태 cookie에 저장
    private val authorizationRequestRepository: CookieOAuth2AuthorizationRequestRepository,
    private val customOAuth2UserService: CustomOAuth2UserService,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {

        http
            // REST API + JWT 구조 → CSRF 비활성화
            .csrf { it.disable() }

            // CORS: Spring MVC 설정 사용
            .cors { }

            // 기본 인증 방식 비활성화
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }

            // OAuth2 Login (세션 ❌, 쿠키 기반 Authorization Request)
            .oauth2Login { oauth ->
                oauth
                    .authorizationEndpoint { endpoint ->
                        endpoint
                            .baseUri("/oauth2/authorization")
                            .authorizationRequestRepository(authorizationRequestRepository)
                    }
                    .redirectionEndpoint { redirection ->
                        redirection.baseUri("/login/oauth2/code/*")
                    }
                    .userInfoEndpoint { userInfo ->
                        userInfo.userService(customOAuth2UserService)
                    }
                    .successHandler(oAuth2LoginSuccessHandler)
            }

            // 인가 정책
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    // OAuth2 로그인 흐름
                    .requestMatchers(
                        "/oauth2/**",
                        "/login/oauth2/**"
                    ).permitAll()

                    // 공개 API
                    .requestMatchers(
                        "/api/health",
                        "/api/public/**"
                    ).permitAll()

                    // 회원가입 / 인증 관련 API
                    .requestMatchers(
                        "/api/auth/signup/**",
                        "/api/auth/refresh",
                        "/api/auth/logout"
                    ).permitAll()
                    .anyRequest().authenticated()
            }

            // 인증 상태를 세션에 저장하지 않음
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

            // 인증 실패 시 401
            .exceptionHandling {
                it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            }

            // JWT 필터 (OAuth 이후 API 요청용)
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        configuration.allowedOrigins = listOf(
            "http://localhost:5173"
        )
        configuration.allowedMethods = listOf(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        )
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
