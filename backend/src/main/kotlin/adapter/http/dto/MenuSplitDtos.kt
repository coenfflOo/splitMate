package adapter.http.dto

// 메뉴 하나에 대한 요청 DTO
data class MenuItemRequest(
    val id: String,
    val name: String,
    val price: String
)

// 더치페이에 참여하는 사람
data class ParticipantRequest(
    val id: String,
    val name: String
)

// 어떤 메뉴를 누가 함께 먹었는지 표현
data class MenuAssignmentRequest(
    val menuId: String,
    val participantIds: List<String>
)

// 메뉴별 더치페이 계산 요청 DTO
data class MenuSplitRequest(
    val currency: String = "CAD",
    val items: List<MenuItemRequest>,
    val participants: List<ParticipantRequest>,
    val assignments: List<MenuAssignmentRequest>,
    val taxAmount: String,
    val tip: TipRequest,
    val exchange: ExchangeOptionRequest
)

// 1명에 대한 메뉴별 분배 결과
data class ParticipantShareResponse(
    val id: String,
    val name: String,
    val subtotalCad: String,
    val taxShareCad: String,
    val tipShareCad: String,
    val totalCad: String,
    val totalKrw: String? = null
)

// 메뉴별 더치페이 계산 응답 DTO
data class MenuSplitResponse(
    val currency: String,
    val totalAmountCad: String,
    val exchange: ExchangeOptionResponse? = null,
    val participants: List<ParticipantShareResponse>
)
