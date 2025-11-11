package adapter.http.dto

/**
 * 환율 옵션 요청 DTO
 *
 * mode:
 *  - "AUTO"  : 외부 환율 API로 CAD → KRW 자동 조회
 *  - "MANUAL": 사용자가 manualRate에 직접 입력
 *  - "NONE"  : KRW 변환 없이 CAD만 보기
 */
data class ExchangeOptionRequest(
    val mode: String,
    val manualRate: String? = null
)

/**
 * 응답에 포함되는 환율 정보 요약
 */
data class ExchangeOptionResponse(
    val mode: String,          // "AUTO", "MANUAL", "NONE"
    val rate: String?,         // 사용된 환율 (AUTO/MANUAL일 때)
    val targetCurrency: String // "KRW" (현재는 원화만)
)
