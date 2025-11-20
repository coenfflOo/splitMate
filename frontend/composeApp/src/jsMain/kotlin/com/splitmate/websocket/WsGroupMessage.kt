package com.splitmate.websocket

import kotlinx.serialization.Serializable

@Serializable
data class WsGroupMessage(
    val roomId: String,
    val members: List<String> = emptyList(),
    val message: String,
    val nextStep: String? = null
)