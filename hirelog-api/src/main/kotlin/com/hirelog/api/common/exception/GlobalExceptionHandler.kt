package com.hirelog.api.common.exception

import com.hirelog.api.common.logging.log
import jakarta.servlet.http.HttpServletRequest
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.context.request.async.AsyncRequestTimeoutException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.io.IOException
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(RequestNotPermitted::class)
    fun handleRequestNotPermitted(
        ex: RequestNotPermitted,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.TOO_MANY_REQUESTS

        log.warn(
            "[RATE_LIMIT_EXCEEDED] path={}, method={}, message={}",
            request.requestURI,
            request.method,
            ex.message
        )

        return ResponseEntity.status(status).body(
            ErrorResponse(
                timestamp = Instant.now(),
                status = status.value(),
                error = "TOO_MANY_REQUESTS",
                message = "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.",
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(AsyncRequestTimeoutException::class)
    fun handleAsyncRequestTimeout(
        ex: AsyncRequestTimeoutException,
        request: HttpServletRequest
    ): ResponseEntity<Void> {
        // SSE 장기 연결 타임아웃은 정상 종료 시나리오이므로 본문 없이 종료한다.
        return ResponseEntity.noContent().build()
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResource(e: NoResourceFoundException): ResponseEntity<Void> {
        return ResponseEntity.notFound().build()
    }

    @ExceptionHandler(IOException::class)
    fun handleIoException(
        ex: IOException,
        request: HttpServletRequest
    ): ResponseEntity<*> {
        if (request.requestURI.startsWith("/api/sse/")) {
            return ResponseEntity.noContent().build<Void>()
        }

        val status = HttpStatus.INTERNAL_SERVER_ERROR

        log.error(
            "[IO_EXCEPTION] path={}, method={}, message={}",
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

    @ExceptionHandler(InvalidCursorException::class)
    fun handleInvalidCursor(
        ex: InvalidCursorException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {

        val status = HttpStatus.BAD_REQUEST

        log.warn(
            "[INVALID_CURSOR] path={}, cursor={}, message={}",
            request.requestURI,
            request.getParameter("cursor"),
            ex.message
        )

        return ResponseEntity.status(status).body(
            ErrorResponse(
                timestamp = Instant.now(),
                status = status.value(),
                error = "INVALID_CURSOR",
                message = ex.message,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFound(
        ex: EntityNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {

        val status = HttpStatus.NOT_FOUND

        log.warn(
            "[ENTITY_NOT_FOUND] path={}, message={}",
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
            "[ENTITY_ALREADY_EXISTS] path={}, message={}",
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

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        ex: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {

        val status = ex.errorCode.status

        log.warn(
            "[BUSINESS_EXCEPTION] path={}, method={}, code={}, message={}",
            request.requestURI,
            request.method,
            ex.errorCode.code,
            ex.message
        )

        return ResponseEntity.status(status).body(
            ErrorResponse(
                timestamp = Instant.now(),
                status = status.value(),
                error = ex.errorCode.code,
                message = ex.message,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.BAD_REQUEST
        val parameterName = ex.name
        val requiredType = ex.requiredType?.simpleName ?: "unknown"
        val rejectedValue = ex.value
        val message = "요청 파라미터 형식이 올바르지 않습니다: $parameterName=$rejectedValue ($requiredType)"

        log.warn(
            "[TYPE_MISMATCH] path={}, method={}, param={}, value={}, requiredType={}, message={}",
            request.requestURI,
            request.method,
            parameterName,
            rejectedValue,
            requiredType,
            ex.message
        )

        return ResponseEntity.status(status).body(
            ErrorResponse(
                timestamp = Instant.now(),
                status = status.value(),
                error = BusinessErrorCode.INVALID_REQUEST_PARAMETER.code,
                message = message,
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

        log.warn(
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

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.BAD_REQUEST
        val message = ex.bindingResult.fieldErrors
            .firstOrNull()
            ?.defaultMessage
            ?: "요청 값이 올바르지 않습니다."

        log.warn(
            "[VALIDATION_FAILED] path={}, method={}, message={}",
            request.requestURI,
            request.method,
            message
        )

        return ResponseEntity.status(status).body(
            ErrorResponse(
                timestamp = Instant.now(),
                status = status.value(),
                error = status.reasonPhrase,
                message = message,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(BindException::class)
    fun handleBindException(
        ex: BindException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.BAD_REQUEST
        val message = ex.bindingResult.fieldErrors
            .firstOrNull()
            ?.defaultMessage
            ?: "요청 파라미터가 올바르지 않습니다."

        log.warn(
            "[BIND_EXCEPTION] path={}, method={}, message={}",
            request.requestURI,
            request.method,
            message
        )

        return ResponseEntity.status(status).body(
            ErrorResponse(
                timestamp = Instant.now(),
                status = status.value(),
                error = status.reasonPhrase,
                message = message,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(
        ex: DataIntegrityViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.BAD_REQUEST
        val message = ex.mostSpecificCause.message
            ?: ex.message
            ?: "데이터 제약조건을 위반했습니다."

        log.warn(
            "[DATA_INTEGRITY_VIOLATION] path={}, method={}, message={}",
            request.requestURI,
            request.method,
            message,
            ex
        )

        return ResponseEntity.status(status).body(
            ErrorResponse(
                timestamp = Instant.now(),
                status = status.value(),
                error = status.reasonPhrase,
                message = message,
                path = request.requestURI
            )
        )
    }

}
