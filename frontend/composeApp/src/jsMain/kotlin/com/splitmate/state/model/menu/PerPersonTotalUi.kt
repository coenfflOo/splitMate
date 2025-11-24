package com.splitmate.state.model.menu

data class PerPersonTotalUi(
    val participantName: String,
    val subtotalCad: String,
    val taxShareCad: String,
    val tipShareCad: String,
    val totalCad: String,
    val totalKrw: String? = null
)