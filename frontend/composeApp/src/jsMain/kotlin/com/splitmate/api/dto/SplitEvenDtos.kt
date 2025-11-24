package com.splitmate.api.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class SplitEvenRequestDto(
    val currency: String = "CAD",
    val totalAmount: String,
    val taxAmount: String,
    val tip: TipRequestDto,
    val peopleCount: Int,
    val exchange: ExchangeOptionRequestDto
)

@Serializable
data class SplitEvenResponseDto(
    @SerialName("totalAmountCad")
    val totalAmountCad: String,
    @SerialName("perPersonCad")
    val perPersonCad: String,
    @SerialName("perPersonKrw")
    val perPersonKrw: String? = null
)