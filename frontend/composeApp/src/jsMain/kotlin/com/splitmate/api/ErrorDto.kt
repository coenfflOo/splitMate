package com.splitmate.api

import kotlinx.serialization.Serializable

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