package com.splitmate.websocket

import kotlinx.serialization.Serializable

@Serializable
data class WsErrorMessage(
    val code: String,
    val message: String
)