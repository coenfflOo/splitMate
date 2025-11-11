package adapter.http.dto

/**
 * 메뉴 하나에 대한 요청 DTO
 */
data class MenuItemRequest(
    val id: String,        // "latte", "americano" 등
    val name: String,      // 표시용 이름
    val price: String      // "6.00" 등 (CAD 기준)
)

/**
 * 더치페이에 참여하는 사람
 */
data class ParticipantRequest(
    val id: String,        // "bella", "alice" 등
    val name: String       // 표시용 이름
)

/**
 * 어떤 메뉴를 누가 함께 먹었는지 표현
 * - menuId: MenuItemRequest.id
 * - participantIds: ParticipantRequest.id 목록
 */
data class MenuAssignmentRequest(
    val menuId: String,
    val participantIds: List<String>
)

/**
 * 메뉴별 더치페이 계산 요청 DTO
 */
data class MenuSplitRequest(
    val currency: String = "CAD",
    val items: List<MenuItemRequest>,
    val participants: List<ParticipantRequest>,
    val assignments: List<MenuAssignmentRequest>,
    val taxAmount: String,
    val tip: TipRequest,
    val exchange: ExchangeOptionRequest
)

/**
 * 1명에 대한 메뉴별 분배 결과
 */
data class ParticipantShareResponse(
    val id: String,
    val name: String,
    val subtotalCad: String,   // 메뉴 가격 합
    val taxShareCad: String,   // 세금 배분
    val tipShareCad: String,   // 팁 배분
    val totalCad: String,      // 최종 CAD
    val totalKrw: String? = null // 선택: KRW 결과
)

/**
 * 메뉴별 더치페이 계산 응답 DTO
 */
data class MenuSplitResponse(
    val currency: String,           // "CAD"
    val totalAmountCad: String,     // 전체 금액 (세금+팁 포함)
    val exchange: ExchangeOptionResponse? = null,
    val participants: List<ParticipantShareResponse>
)
