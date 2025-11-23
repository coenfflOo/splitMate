package adapter.http.dto

data class TipRequest(
    val mode: String,
    val percent: Int? = null,
    val absolute: String? = null
)