package com.splitmate.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class TipRequestDto(
    val mode: String,
    val percent: Int? = null,
    val absolute: String? = null
)