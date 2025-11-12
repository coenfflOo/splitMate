package adapter.http.dto

data class ErrorResponse(
    val error: ErrorBody
)

data class ErrorBody(
    val code: String,                    // 예: "INVALID_INPUT"
    val message: String,                 // 예: "peopleCount must be >= 1"
    val details: Map<String, Any?>? = null
)
