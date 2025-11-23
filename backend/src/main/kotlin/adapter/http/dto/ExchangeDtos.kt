package adapter.http.dto

data class ExchangeOptionRequest(
    val mode: String,
    val manualRate: String? = null
)

data class ExchangeOptionResponse(
    val mode: String,
    val rate: String?,
    val targetCurrency: String
)