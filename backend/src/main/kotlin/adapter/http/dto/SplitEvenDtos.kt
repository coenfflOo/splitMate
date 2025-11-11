package adapter.http.dto

/**
 * N분의 1 더치페이 계산 요청 DTO
 */
data class SplitEvenRequest(
    val currency: String = "CAD",  // 기본 CAD
    val totalAmount: String,       // 총 금액 (예: "27.40")
    val taxAmount: String,         // 세금 금액 (예: "2.60")
    val tip: TipRequest,           // 팁 정보
    val peopleCount: Int,          // 인원 수 (N)
    val exchange: ExchangeOptionRequest // 환율 옵션
)

/**
 * N분의 1 더치페이 계산 응답 DTO
 */
data class SplitEvenResponse(
    val currency: String,          // "CAD"
    val totalAmountCad: String,    // 최종 총액 (세금+팁 포함)
    val peopleCount: Int,          // 인원 수
    val perPersonCad: String,      // 1인당 CAD 금액
    val exchange: ExchangeOptionResponse? = null,
    val perPersonKrw: String? = null
)
