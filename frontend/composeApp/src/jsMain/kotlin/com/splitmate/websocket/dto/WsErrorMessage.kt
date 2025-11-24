package com.splitmate.websocket.dto

import kotlinx.serialization.Serializable

@Serializable
data class WsErrorMessage(
    val code: String,
    val message: String
)