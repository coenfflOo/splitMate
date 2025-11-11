package adapter.http.dto

/**
 * 클라이언트에서 들어오는 팁 정보 DTO
 *
 * mode:
 *  - "PERCENT"  : percent 필드 사용 (예: 15)
 *  - "ABSOLUTE" : absolute 필드 사용 (예: "10.00")
 *  - "NONE"     : 팁 없음
 */
data class TipRequest(
    val mode: String,
    val percent: Int? = null,
    val absolute: String? = null
)

/**
 * 응답에서 팁 정보를 굳이 내려줄 필요가 없다면 생략 가능하지만,
 * 나중에 "어떤 팁 기준으로 계산되었는지"를 보여주고 싶다면 사용할 수 있는 DTO.
 */
data class TipSummaryResponse(
    val mode: String,
    val percent: Int? = null,
    val absolute: String? = null
)
