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
data class TipDto(
    val mode: String,
    val percent: Double? = null,
    val absolute: String? = null
)

@Serializable
data class ExchangeDto(
    val mode: String,
    val manualRate: String? = null
)