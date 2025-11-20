package com.splitmate.state

data class SoloSplitUiState(
    val step: SoloStep = SoloStep.TOTAL_AMOUNT,

    // 총 금액 입력
    val amountInput: String = "",
    val amountError: String? = null,

    // 세금 입력
    val taxInput: String = "",
    val taxError: String? = null,

    val tipMode: SoloTipMode? = null,
    val tipModeError: String? = null,

    val tipValueInput: String = "",
    val tipValueError: String? = null,

    val peopleCountInput: String = "",
    val peopleCountError: String? = null
) {
    val canProceedFromTotal: Boolean
        get() = amountInput.isNotBlank() && amountError == null

    val canProceedFromTax: Boolean
        get() = taxInput.isNotBlank() && taxError == null

    val canProceedFromTipValue: Boolean
        get() = tipMode == SoloTipMode.NONE || (tipValueInput.isNotBlank() && tipValueError == null)
}