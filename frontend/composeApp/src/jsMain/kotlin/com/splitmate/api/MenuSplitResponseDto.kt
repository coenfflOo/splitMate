package com.splitmate.api

import kotlinx.serialization.Serializable

@Serializable
data class MenuSplitResponseDto(
    val currency: String,
    val totalAmountCad: String,
    val exchange: ExchangeOptionDto? = null,
    val participants: List<ParticipantShareDto>
)

@Serializable
data class ExchangeOptionDto(
    val mode: String,
    val rate: String?,
    val targetCurrency: String
)

@Serializable
data class ParticipantShareDto(
    val id: String,
    val name: String,
    val subtotalCad: String,
    val taxShareCad: String,
    val tipShareCad: String,
    val totalCad: String,
    val totalKrw: String? = null
)
