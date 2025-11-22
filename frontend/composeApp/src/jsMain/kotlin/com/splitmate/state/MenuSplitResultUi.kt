package com.splitmate.state

data class MenuSplitResultUi(
    val totalAmountCad: String,
    val exchangeMode: String? = null,
    val exchangeRate: String? = null,
    val perPersonTotals: List<PerPersonTotalUi>
)
