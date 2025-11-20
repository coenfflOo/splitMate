package com.splitmate.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class SoloSplitUiState(
    val amountInput: String = "",
    val amountError: String? = null
) {
    val canProceed: Boolean
        get() = amountInput.isNotBlank() && amountError == null
}

class SoloSplitViewModel {

    var uiState by mutableStateOf(SoloSplitUiState())
        private set

    fun onAmountChange(input: String) {
        val trimmed = input.trim()
        val error = validateAmount(trimmed)
        uiState = uiState.copy(
            amountInput = input,   // 원본 그대로 유지 (사용자 입력 형태)
            amountError = error
        )
    }

    fun onSubmit(): Boolean {
        val trimmed = uiState.amountInput.trim()
        val error = validateAmount(trimmed)
        uiState = uiState.copy(amountError = error)
        return error == null
    }

    private fun validateAmount(input: String): String? {
        if (input.isBlank()) {
            return "총 결제 금액을 입력해주세요."
        }

        // 쉼표 제거 후 숫자 체크
        val numeric = input.replace(",", "")
        val value = numeric.toDoubleOrNull()
            ?: return "숫자 형식으로 입력해주세요. 예: 27.40"

        if (value <= 0.0) {
            return "0보다 큰 금액을 입력해주세요."
        }

        // 여기서 소수점 자리수 정책을 추가해도 됨 (예: 둘째 자리까지만 허용)
        return null
    }
}
