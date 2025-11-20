package com.splitmate.websocket

import kotlinx.serialization.Serializable

@Serializable
data class GroupInputPayload(
    val memberId: String,
    val input: String
)