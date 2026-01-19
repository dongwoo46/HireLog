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


    /**
     * 공통 Entity Not Found 처리
     *
     * - 모든 도메인의 필수 엔티티 조회 실패
     * - REST API 기준 404 Not Found
     */
    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFound(
        ex: EntityNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {

        val status = HttpStatus.NOT_FOUND

        return ResponseEntity.status(status).body(
            ErrorResponse(
                timestamp = Instant.now(),
                status = status.value(),
                error = ex.message ?: status.reasonPhrase,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(EntityAlreadyExistsException::class)
    fun handleEntityExists(
        ex: EntityAlreadyExistsException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {

        val status = HttpStatus.CONFLICT

        return ResponseEntity.status(status).body(
            ErrorResponse(
                timestamp = Instant.now(),
                status = status.value(),
                error = ex.message ?: status.reasonPhrase,
                path = request.requestURI
            )
        )
    }


    @ExceptionHandler(GeminiCallException::class)
    fun handleGeminiCall(
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
}
