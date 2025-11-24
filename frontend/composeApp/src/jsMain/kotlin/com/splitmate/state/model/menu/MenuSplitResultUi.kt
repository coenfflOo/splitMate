package com.splitmate.state.model.menu

data class MenuSplitResultUi(
    val totalAmountCad: String,
    val exchangeMode: String? = null,
    val exchangeRate: String? = null,
    val perPersonTotals: List<PerPersonTotalUi>
)