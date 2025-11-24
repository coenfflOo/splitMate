package com.splitmate.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class GroupCreateRoomRequestDto(
    val roomId: String,
    val memberId: String
)

@Serializable
data class GroupJoinRoomRequestDto(
    val memberId: String
)

@Serializable
data class GroupRoomResponseDto(
    val roomId: String,
    val members: List<String> = emptyList(),
    val message: String,
    val nextStep: String,
    val senderId: String? = null,
    val messageType: String = "SYSTEM"
)
