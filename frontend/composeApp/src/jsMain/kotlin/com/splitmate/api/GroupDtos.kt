package com.splitmate.api

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
data class GroupMessageRequestDto(
    val memberId: String,
    val input: String
)

@Serializable
data class GroupRoomResponseDto(
    val roomId: String,
    val members: List<String> = emptyList(),
    val message: String,
    val nextStep: String
)

@Serializable
data class ErrorBodyDto(
    val code: String,
    val message: String,
    val details: String? = null
)

@Serializable
data class ErrorResponseDto(
    val error: ErrorBodyDto
)
