package adapter.http.dto

data class ErrorResponse(
    val error: ErrorBody
)

data class ErrorBody(
    val code: String,
    val message: String,
    val details: Map<String, Any?>? = null
)

