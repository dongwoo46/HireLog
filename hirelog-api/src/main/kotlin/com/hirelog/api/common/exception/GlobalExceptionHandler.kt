package com.hirelog.api.common.exception

import com.hirelog.api.common.logging.log
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResource(e: NoResourceFoundException): ResponseEntity<Void> {
        return ResponseEntity.notFound().build()
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFound(
        ex: EntityNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {

        val status = HttpStatus.NOT_FOUND

        log.warn(
            "[ENTITY_NOT_FOUND] path={} message={}",
            request.requestURI,
            ex.message
        )

        return ResponseEntity.status(status).body(
            ErrorResponse(
                timestamp = Instant.now(),
                status = status.value(),
                error = status.reasonPhrase,
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

        log.warn(
            "[ENTITY_ALREADY_EXISTS] path={} message={}",
            request.requestURI,
            ex.message
        )

        return ResponseEntity.status(status).body(
            ErrorResponse(
                timestamp = Instant.now(),
                status = status.value(),
                error = status.reasonPhrase,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {

        val status = HttpStatus.INTERNAL_SERVER_ERROR

        log.error(
            "[UNHANDLED_EXCEPTION] path={}, method={}, exceptionType={}, message={}",
            request.requestURI,
            request.method,
            ex.javaClass.simpleName,
            ex.message,
            ex
        )

        return ResponseEntity.status(status).body(
            ErrorResponse(
                timestamp = Instant.now(),
                status = status.value(),
                error = status.reasonPhrase,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {

        val status = HttpStatus.BAD_REQUEST

        log.error(
            "[ILLEGAL_ARGUMENT] path={}, method={}, message={}",
            request.requestURI,
            request.method,
            ex.message,
            ex
        )

        return ResponseEntity.status(status).body(
            ErrorResponse(
                timestamp = Instant.now(),
                status = status.value(),
                error = status.reasonPhrase,
                message = ex.message,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(
        ex: IllegalStateException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {

        val status = HttpStatus.INTERNAL_SERVER_ERROR

        log.error(
            "[ILLEGAL_STATE] path={}, method={}, message={}",
            request.requestURI,
            request.method,
            ex.message,
            ex
        )

        return ResponseEntity.status(status).body(
            ErrorResponse(
                timestamp = Instant.now(),
                status = status.value(),
                error = status.reasonPhrase,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(VerificationCodeExpiredException::class)
    fun handleVerificationCodeExpired(
        ex: VerificationCodeExpiredException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {

        val status = HttpStatus.GONE

        return ResponseEntity
            .status(status)
            .body(
                ErrorResponse(
                    timestamp = Instant.now(),
                    status = status.value(),
                    error = "VERIFICATION_CODE_EXPIRED",
                    path = request.requestURI,
                    message = "인증 코드가 만료되었습니다."
                )
            )
    }

    @ExceptionHandler(InvalidVerificationCodeException::class)
    fun handleInvalidVerificationCode(
        ex: InvalidVerificationCodeException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    timestamp = Instant.now(),
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "INVALID_VERIFICATION_CODE",
                    path = request.requestURI,
                    message = ex.message
                )
            )
    }

}
