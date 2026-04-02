package com.hirelog.api.auth.presentation

import com.hirelog.api.auth.application.PasswordResetService
import com.hirelog.api.auth.presentation.dto.PasswordResetReq
import com.hirelog.api.auth.presentation.dto.PasswordResetSendCodeReq
import com.hirelog.api.auth.presentation.dto.PasswordResetVerifyCodeReq
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth/password")
class PasswordResetController(
    private val passwordResetService: PasswordResetService,
) {
    @PostMapping("/send-code")
    fun sendCode(
        @Valid @RequestBody request: PasswordResetSendCodeReq
    ): ResponseEntity<Void> {
        passwordResetService.sendCode(request.email)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/verify-code")
    fun verifyCode(
        @Valid @RequestBody request: PasswordResetVerifyCodeReq
    ): ResponseEntity<Void> {
        passwordResetService.verifyCode(
            email = request.email,
            code = request.code
        )
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/reset")
    fun resetPassword(
        @Valid @RequestBody request: PasswordResetReq
    ): ResponseEntity<Void> {
        passwordResetService.resetPassword(
            email = request.email,
            newPassword = request.newPassword
        )
        return ResponseEntity.ok().build()
    }
}

