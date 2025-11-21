package com.splitmate.model

import kotlinx.serialization.Serializable

@Serializable
data class ConversationOutputDto(
    val message: String,
    val nextStep: String?,
    val finished: Boolean
)

@Serializable
data class GroupRoomResponseDto(
    val roomId: String,
    val members: List<String>,
    val message: String,
    val nextStep: String?
)
