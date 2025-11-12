package adapter.http.dto

// 클라이언트에서 들어오는 팁 정보 DTO
data class TipRequest(
    val mode: String,
    val percent: Int? = null,
    val absolute: String? = null
)

data class TipSummaryResponse(
    val mode: String,
    val percent: Int? = null,
    val absolute: String? = null
)
