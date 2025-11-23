package adapter.http.dto

data class MenuItemRequest(
    val id: String,
    val name: String,
    val price: String
)

data class ParticipantRequest(
    val id: String,
    val name: String
)

data class MenuAssignmentRequest(
    val menuId: String,
    val participantIds: List<String>
)

data class MenuSplitRequest(
    val currency: String = "CAD",
    val items: List<MenuItemRequest>,
    val participants: List<ParticipantRequest>,
    val assignments: List<MenuAssignmentRequest>,
    val taxAmount: String,
    val tip: TipRequest,
    val exchange: ExchangeOptionRequest
)

data class ParticipantShareResponse(
    val id: String,
    val name: String,
    val subtotalCad: String,
    val taxShareCad: String,
    val tipShareCad: String,
    val totalCad: String,
    val totalKrw: String? = null
)

data class MenuSplitResponse(
    val currency: String,
    val totalAmountCad: String,
    val exchange: ExchangeOptionResponse? = null,
    val participants: List<ParticipantShareResponse>
)
