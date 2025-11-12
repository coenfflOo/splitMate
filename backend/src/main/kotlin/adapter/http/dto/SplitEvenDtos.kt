package adapter.http.dto

// N분의 1 더치페이 계산 요청 DTO
data class SplitEvenRequest(
    val currency: String = "CAD",
    val totalAmount: String,
    val taxAmount: String,
    val tip: TipRequest,
    val peopleCount: Int,
    val exchange: ExchangeOptionRequest
)

// N분의 1 더치페이 계산 응답 DTO
data class SplitEvenResponse(
    val currency: String,
    val totalAmountCad: String,
    val peopleCount: Int,
    val perPersonCad: String,
    val exchange: ExchangeOptionResponse? = null,
    val perPersonKrw: String? = null
)
