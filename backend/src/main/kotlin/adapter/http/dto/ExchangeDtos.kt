package adapter.http.dto

// 환율 옵션 요청 DTO
data class ExchangeOptionRequest(
    val mode: String,
    val manualRate: String? = null
)

// 응답에 포함되는 환율 정보 요약
data class ExchangeOptionResponse(
    val mode: String,
    val rate: String?,
    val targetCurrency: String
)
