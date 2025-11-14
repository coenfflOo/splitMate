package adapter.http.dto

data class GroupMessageRequest(
    val memberId: String,
    val input: String
)