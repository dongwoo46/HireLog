package com.hirelog.api.common.exception

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(GeminiParseException::class)
    fun handleGeminiParse(
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {

        val status = HttpStatus.BAD_GATEWAY

        return ResponseEntity.status(status).body(
            ErrorResponse(
                timestamp = Instant.now(),
                status = status.value(),
                error = status.reasonPhrase,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(DuplicateJobSnapshotException::class)
    fun handleDuplicateJobSnapshot(
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {

        val status = HttpStatus.CONFLICT

        return ResponseEntity.status(status).body(
            ErrorResponse(
                timestamp = Instant.now(),
                status = status.value(),
                error = status.reasonPhrase,
                path = request.requestURI
            )
        )
    }
}
