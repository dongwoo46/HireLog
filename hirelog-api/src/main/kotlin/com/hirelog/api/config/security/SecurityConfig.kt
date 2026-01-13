package com.hirelog.api.config.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // CSRF 비활성화 (Postman 테스트)
            .csrf { it.disable() }

            // 모든 요청 허용
            .authorizeHttpRequests {
                it.anyRequest().permitAll()
            }

            // 기본 인증 메커니즘 비활성화
            .httpBasic { it.disable() }
            .formLogin { it.disable() }

        return http.build()
    }
}
