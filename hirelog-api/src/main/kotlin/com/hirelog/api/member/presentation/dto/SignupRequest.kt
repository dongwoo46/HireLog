package com.hirelog.api.member.presentation.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 이메일 중복 체크 요청
 */
data class CheckEmailRequest(
    @field:NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @field:Email(message = "유효한 이메일 형식이 아닙니다.")
    val email: String,
)

/**
 * 이메일 중복 체크 응답
 */
data class CheckEmailResponse(
    val exists: Boolean,
)

/**
 * 기존 계정 연동 요청
 */
data class BindRequest(
    @field:NotBlank(message = "연동할 이메일은 필수입니다.")
    @field:Email(message = "유효한 이메일 형식이 아닙니다.")
    val email: String,
)

/**
 * 인증코드 발송 요청 (기존 계정 연결 선택 시)
 */
data class SendCodeRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "유효한 이메일 형식이 아닙니다.")
    val email: String,
)

/**
 * 인증코드 검증 요청
 */
data class VerifyCodeRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "유효한 이메일 형식이 아닙니다.")
    val email: String,

    @field:NotBlank(message = "인증코드는 필수입니다.")
    @field:Size(min = 6, max = 6, message = "인증코드는 6자리입니다.")
    val code: String,
)

/**
 * 인증코드 검증 응답
 */
data class VerifyCodeResponse(
    val verified: Boolean,
)

/**
 * 신규 회원가입 완료 요청
 */
data class SignupCompleteRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "유효한 이메일 형식이 아닙니다.")
    val email: String,

    @field:NotBlank(message = "닉네임은 필수입니다.")
    @field:Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하로 입력해주세요.")
    val username: String,

    val currentPositionId: Long? = null,

    @field:Min(value = 0, message = "경력 연차는 음수일 수 없습니다.")
    val careerYears: Int? = null,

    @field:Size(max = 1000, message = "자기소개는 1000자 이내로 입력해주세요.")
    val summary: String? = null,
)

