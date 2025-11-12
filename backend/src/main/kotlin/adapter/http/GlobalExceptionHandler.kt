package adapter.http

import adapter.http.dto.ErrorBody
import adapter.http.dto.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        val body = ErrorResponse(
            error = ErrorBody(
                code = "INVALID_INPUT",
                message = e.message ?: "Invalid input"
            )
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }
}
