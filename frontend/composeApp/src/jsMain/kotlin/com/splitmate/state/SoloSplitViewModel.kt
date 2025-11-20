package com.splitmate.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// SOLO 모드 내부 단계
enum class SoloStep {
    TOTAL_AMOUNT,
    TAX
}

data class SoloSplitUiState(
    val step: SoloStep = SoloStep.TOTAL_AMOUNT,

    // 총 금액 입력
    val amountInput: String = "",
    val amountError: String? = null,

    // 세금 입력
    val taxInput: String = "",
    val taxError: String? = null
) {
    val canProceedFromTotal: Boolean
        get() = amountInput.isNotBlank() && amountError == null

    val canProceedFromTax: Boolean
        get() = taxInput.isNotBlank() && taxError == null
}

class SoloSplitViewModel {

    var uiState by mutableStateOf(SoloSplitUiState())
        private set

    // ---------------- 총 금액 ----------------

    fun onAmountChange(input: String) {
        val trimmed = input.trim()
        val error = validateAmount(trimmed)
        uiState = uiState.copy(
            amountInput = input,
            amountError = error
        )
    }

    fun onTotalSubmit(): Boolean {
        val trimmed = uiState.amountInput.trim()
        val error = validateAmount(trimmed)

        uiState = uiState.copy(amountError = error)

        if (error != null) return false

        // 총 금액이 유효하면 세금 단계로 진행
        uiState = uiState.copy(step = SoloStep.TAX)
        return true
    }

    private fun validateAmount(input: String): String? {
        if (input.isBlank()) {
            return "총 결제 금액을 입력해주세요."
        }

        val numeric = input.replace(",", "")
        val value = numeric.toDoubleOrNull()
            ?: return "숫자 형식으로 입력해주세요. 예: 27.40"

        if (value <= 0.0) {
            return "0보다 큰 금액을 입력해주세요."
        }

        return null
    }

    // ---------------- 세금 ----------------

    fun onTaxChange(input: String) {
        val trimmed = input.trim()
        val error = validateTax(trimmed)
        uiState = uiState.copy(
            taxInput = input,
            taxError = error
        )
    }

    fun onTaxNoneClick() {
        // UI에서 "세금 없음" 버튼 눌렀을 때
        val value = "없음"
        val error = validateTax(value)
        uiState = uiState.copy(
            taxInput = value,
            taxError = error
        )
    }

    fun onTaxSubmit(): Boolean {
        val trimmed = uiState.taxInput.trim()
        val error = validateTax(trimmed)

        uiState = uiState.copy(taxError = error)

        if (error != null) return false

        // TODO: 다음 단계(팁 입력)로 진행 예정
        return true
    }

    private fun validateTax(input: String): String? {
        if (input.isBlank()) {
            return "세금이 없으면 '없음'을 선택하거나 0을 입력해주세요."
        }

        val normalized = input.trim().lowercase()

        // 없음 케이스
        if (normalized == "없음" || normalized == "none" || normalized == "no") {
            return null
        }

        // 숫자 케이스
        val numeric = input.replace(",", "")
        val value = numeric.toDoubleOrNull()
            ?: return "숫자 또는 '없음'으로 입력해주세요."

        if (value < 0.0) {
            return "세금 금액은 0 이상이어야 합니다."
        }

        return null
    }
}
