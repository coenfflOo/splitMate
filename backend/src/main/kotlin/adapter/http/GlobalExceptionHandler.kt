package adapter.http

import adapter.http.dto.ErrorBody
import adapter.http.dto.ErrorResponse
import application.group.RoomNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(RoomNotFoundException::class)
    fun handleRoomNotFound(e: RoomNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(
                error = ErrorBody(
                    code = "ROOM_NOT_FOUND",
                    message = e.message ?: "Room not found"
                )
            )
        )

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                error = ErrorBody(
                    code = "INVALID_INPUT",
                    message = e.message ?: "Invalid input"
                )
            )
        )

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(e: IllegalStateException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse(
                error = ErrorBody(
                    code = "CONTEXT_MISSING",
                    message = e.message ?: "Invalid conversation state"
                )
            )
        )
}
