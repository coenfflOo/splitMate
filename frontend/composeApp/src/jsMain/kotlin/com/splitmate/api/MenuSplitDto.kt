package com.splitmate.api

import kotlinx.serialization.Serializable

@Serializable
data class MenuSplitRequestDto(
    val currency: String = "CAD",
    val items: List<MenuItemDto>,
    val participants: List<MenuParticipantDto>,
    val assignments: List<MenuAssignmentDto>,
    val taxAmount: String,
    val tip: TipRequestDto,
    val exchange: ExchangeOptionRequestDto
)

@Serializable
data class MenuItemDto(
    val id: String,
    val name: String,
    val price: String
)

@Serializable
data class MenuParticipantDto(
    val id: String,
    val name: String
)

@Serializable
data class MenuAssignmentDto(
    val menuId: String,
    val participantIds: List<String>
)

@Serializable
data class MenuSplitResponseDto(
    val currency: String,
    val totalAmountCad: String,
    val exchange: ExchangeOptionDto? = null,
    val participants: List<ParticipantShareDto>
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
