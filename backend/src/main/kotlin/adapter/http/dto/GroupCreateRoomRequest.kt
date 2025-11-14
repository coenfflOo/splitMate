package adapter.http.dto

data class GroupCreateRoomRequest(
    val roomId: String,
    val memberId: String
)