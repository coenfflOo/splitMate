package adapter.http.dto

data class GroupDto(
    val roomId: String,
    val memberId: String
)

data class GroupJoinRoomRequest(
    val memberId: String
)

data class GroupMessageRequest(
    val memberId: String,
    val input: String
)

data class GroupRoomResponse(
    val roomId: String,
    val members: List<String>,
    val message: String,
    val nextStep: String,
    val senderId: String? = null,
    val messageType: String = "SYSTEM"
)