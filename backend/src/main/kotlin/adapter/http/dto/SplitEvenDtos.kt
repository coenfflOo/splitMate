package adapter.http.dto

data class SplitEvenRequest(
    val currency: String = "CAD",
    val totalAmount: String,
    val taxAmount: String,
    val tip: TipRequest,
    val peopleCount: Int,
    val exchange: ExchangeOptionRequest
)

data class SplitEvenResponse(
    val currency: String,
    val totalAmountCad: String,
    val peopleCount: Int,
    val perPersonCad: String,
    val exchange: ExchangeOptionResponse? = null,
    val perPersonKrw: String? = null
)
