package com.splitmate.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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

        uiState = uiState.copy(step = SoloStep.TIP_MODE)
        return true
    }

    private fun validateTax(input: String): String? {
        if (input.isBlank()) {
            return "세금이 없으면 '없음'을 선택하거나 0을 입력해주세요."
        }

        val normalized = input.trim().lowercase()

        if (normalized == "없음" || normalized == "none" || normalized == "no") {
            return null
        }

        val numeric = input.replace(",", "")
        val value = numeric.toDoubleOrNull()
            ?: return "숫자 또는 '없음'으로 입력해주세요."

        if (value < 0.0) {
            return "세금 금액은 0 이상이어야 합니다."
        }

        return null
    }

    fun onTipModeSelected(mode: SoloTipMode) {
        uiState = uiState.copy(
            tipMode = mode,
            tipModeError = null
        )
    }

    fun onTipModeProceed() {
        val mode = uiState.tipMode
        if (mode == null) {
            uiState = uiState.copy(
                tipModeError = "팁 입력 방식을 선택해주세요."
            )
            return
        }

        uiState = when (mode) {
            SoloTipMode.PERCENT,
            SoloTipMode.ABSOLUTE -> uiState.copy(step = SoloStep.TIP_VALUE)

            SoloTipMode.NONE -> uiState.copy(
                // 팁 없이 진행 → 바로 분배 방식 단계로 (2-5에서 구현)
                step = SoloStep.SPLIT_MODE
            )
        }
    }

    fun onTipValueChange(input: String) {
        val trimmed = input.trim()
        val error = validateTipValue(trimmed)

        uiState = uiState.copy(
            tipValueInput = input,
            tipValueError = error
        )
    }

    fun onTipValueSubmit(): Boolean {
        val trimmed = uiState.tipValueInput.trim()
        val error = validateTipValue(trimmed)

        uiState = uiState.copy(tipValueError = error)

        if (error != null) return false

        // TODO: 다음 단계(분배 방식 선택)로 이동 (2-5에서 구현)
        uiState = uiState.copy(step = SoloStep.SPLIT_MODE)
        return true
    }

    private fun validateTipValue(input: String): String? {
        val mode = uiState.tipMode

        // 안전망: 모드가 없으면 에러
        if (mode == null) {
            return "먼저 팁 입력 방식을 선택해주세요."
        }

        // NONE 모드인 경우 여기까지 오지 않는 것이 정상
        if (mode == SoloTipMode.NONE) {
            return null
        }

        if (input.isBlank()) {
            return "팁 값을 입력해주세요."
        }

        val numeric = input.replace(",", "")
        val number = numeric.toDoubleOrNull()
            ?: return "숫자 형식으로 입력해주세요."

        return when (mode) {
            SoloTipMode.PERCENT -> {
                if (number < 0.0 || number > 100.0) {
                    "0 ~ 100 사이의 퍼센트만 입력할 수 있습니다."
                } else null
            }

            SoloTipMode.ABSOLUTE -> {
                if (number <= 0.0) {
                    "0보다 큰 금액을 입력해주세요."
                } else null
            }

            SoloTipMode.NONE -> null
        }
    }

}
