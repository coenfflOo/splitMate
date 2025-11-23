package com.splitmate.api

import kotlinx.serialization.Serializable

@Serializable
data class ExchangeOptionRequestDto(
    val mode: String,
    val manualRate: String? = null
)

@Serializable
data class ExchangeOptionDto(
    val mode: String,
    val rate: String?,
    val targetCurrency: String
)