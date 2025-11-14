package adapter.http.dto

data class GroupRoomResponse(
    val roomId: String,
    val members: List<String>,
    val message: String,
    val nextStep: String
)