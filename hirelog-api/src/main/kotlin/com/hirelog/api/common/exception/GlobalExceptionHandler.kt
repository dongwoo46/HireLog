package com.hirelog.api.common.exception

import com.hirelog.api.common.logging.log
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

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
            "[UNHANDLED_EXCEPTION] path={} exception={}",
            request.requestURI,
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
}
