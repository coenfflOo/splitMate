package com.splitmate.state.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.splitmate.api.dto.ExchangeOptionRequestDto
import com.splitmate.api.dto.SplitEvenRequestDto
import com.splitmate.api.dto.TipRequestDto
import com.splitmate.api.callSplitEven
import com.splitmate.state.model.solo.SoloExchangeMode
import com.splitmate.state.model.solo.SoloResult
import com.splitmate.state.model.solo.SoloTipMode
import com.splitmate.state.steps.SoloStep
import com.splitmate.state.uistate.SoloSplitUiState
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class SoloSplitViewModel {

    private val scope = MainScope()
    var uiState by mutableStateOf(SoloSplitUiState())
        private set

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

    fun onTaxChange(input: String) {
        val trimmed = input.trim()
        val error = validateTax(trimmed)
        uiState = uiState.copy(
            taxInput = input,
            taxError = error
        )
    }

    fun onTaxNoneClick() {
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

        uiState = uiState.copy(step = SoloStep.SPLIT_MODE)
        return true
    }

    private fun validateTipValue(input: String): String? {
        val mode = uiState.tipMode

        if (mode == null) {
            return "먼저 팁 입력 방식을 선택해주세요."
        }

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

    fun onSplitModeNDivideSelected() {
        uiState = uiState.copy(
            step = SoloStep.PEOPLE_COUNT
        )
    }

    fun onPeopleCountChange(input: String) {
        uiState = uiState.copy(
            peopleCountInput = input,
            peopleCountError = null
        )
    }

    fun onPeopleCountSubmit(): Boolean {
        val raw = uiState.peopleCountInput.trim()
        val error = validatePeopleCount(raw)

        if (error != null) {
            uiState = uiState.copy(peopleCountError = error)
            return false
        }

        uiState = uiState.copy(
            peopleCountError = null,
            step = SoloStep.EXCHANGE_RATE_MODE
        )
        return true
    }

    private fun validatePeopleCount(input: String): String? {
        if (input.isBlank()) {
            return "인원 수를 입력해주세요."
        }

        val n = input.toIntOrNull()
            ?: return "인원 수는 숫자로 입력해주세요."

        if (n < 1) {
            return "인원 수는 1 이상의 정수여야 합니다."
        }

        return null
    }

    fun onExchangeModeSelected(mode: SoloExchangeMode) {
        uiState = uiState.copy(
            exchangeMode = mode,
            exchangeModeError = null
        )
    }

    fun onExchangeModeSubmit(): Boolean {
        val mode = uiState.exchangeMode
        if (mode == null) {
            uiState = uiState.copy(
                exchangeModeError = "환율 모드를 선택해주세요."
            )
            return false
        }

        uiState = when (mode) {
            SoloExchangeMode.MANUAL ->
                uiState.copy(
                    exchangeModeError = null,
                    step = SoloStep.EXCHANGE_RATE_VALUE
                )

            SoloExchangeMode.AUTO,
            SoloExchangeMode.NONE ->
                uiState.copy(
                    exchangeModeError = null,
                    step = SoloStep.RESULT
                )
        }
        return true
    }

    fun onExchangeRateValueChange(input: String) {
        val trimmed = input.trim()
        val error = validateExchangeRateValue(trimmed)

        uiState = uiState.copy(
            exchangeRateInput = input,
            exchangeRateError = error
        )
    }

    fun onExchangeRateValueSubmit(): Boolean {
        val trimmed = uiState.exchangeRateInput.trim()
        val error = validateExchangeRateValue(trimmed)

        uiState = uiState.copy(exchangeRateError = error)

        if (error != null) return false

        uiState = uiState.copy(step = SoloStep.RESULT)
        return true
    }

    private fun validateExchangeRateValue(input: String): String? {
        if (input.isBlank()) {
            return "환율 값을 입력해주세요."
        }

        val numeric = input.replace(",", "")
        val value = numeric.toDoubleOrNull()
            ?: return "숫자 형식으로 입력해주세요. 예: 1000"

        if (value <= 0.0) {
            return "0보다 큰 값을 입력해주세요."
        }

        return null
    }

    private fun buildSplitEvenRequestOrNull(): SplitEvenRequestDto? {
        val s = uiState

        val baseStr = s.amountInput.replace(",", "").trim()
        val base = baseStr.toDoubleOrNull() ?: return null

        val taxStrRaw = s.taxInput.trim()
        val taxStr = when (taxStrRaw.lowercase()) {
            "없음", "none", "no" -> "0"
            else -> taxStrRaw.replace(",", "")
        }
        taxStr.toDoubleOrNull() ?: return null

        val tipModeEnum = s.tipMode ?: SoloTipMode.NONE
        val tipDto = when (tipModeEnum) {
            SoloTipMode.PERCENT -> {
                val p = s.tipValueInput.replace(",", "").toDoubleOrNull() ?: return null
                TipRequestDto(mode = "PERCENT", percent = p.toInt())
            }

            SoloTipMode.ABSOLUTE -> {
                val abs = s.tipValueInput.replace(",", "").toDoubleOrNull() ?: return null
                TipRequestDto(mode = "ABSOLUTE", absolute = abs.toString())
            }

            SoloTipMode.NONE -> TipRequestDto(mode = "NONE")
        }

        val people = s.peopleCountInput.trim().toIntOrNull() ?: return null
        if (people <= 0) return null

        val exchangeModeEnum = s.exchangeMode ?: SoloExchangeMode.NONE
        val exchangeDto = when (exchangeModeEnum) {
            SoloExchangeMode.AUTO -> ExchangeOptionRequestDto(mode = "AUTO")
            SoloExchangeMode.NONE -> ExchangeOptionRequestDto(mode = "NONE")
            SoloExchangeMode.MANUAL -> {
                val rateStr = s.exchangeRateInput.replace(",", "").trim()
                rateStr.toDoubleOrNull() ?: return null
                ExchangeOptionRequestDto(mode = "MANUAL", manualRate = rateStr)
            }
        }

        return SplitEvenRequestDto(
            currency = "CAD",
            totalAmount = base.toString(),
            taxAmount = taxStr,
            tip = tipDto,
            peopleCount = people,
            exchange = exchangeDto
        )
    }

    fun requestBackendResult() {
        if (uiState.isLoading || uiState.result != null) return

        val request = buildSplitEvenRequestOrNull()
        if (request == null) {
            uiState = uiState.copy(
                apiError = "입력 값이 유효하지 않아 계산 요청을 보낼 수 없습니다."
            )
            return
        }

        scope.launch {
            uiState = uiState.copy(
                isLoading = true,
                apiError = null,
                result = null
            )

            runCatching {
                callSplitEven(request)
            }.onSuccess { response ->
                val totalCadStr = "${response.totalAmountCad} CAD"
                val perPersonCadStr = "${response.perPersonCad} CAD"

                val perPersonKrwStr = response.perPersonKrw?.let { krw ->
                    "$krw KRW"
                }

                uiState = uiState.copy(
                    isLoading = false,
                    result = SoloResult(
                        totalCad = totalCadStr,
                        perPersonCad = perPersonCadStr,
                        perPersonKrw = perPersonKrwStr
                    )
                )
            }.onFailure { e ->
                uiState = uiState.copy(
                    isLoading = false,
                    apiError = "계산 요청 중 오류가 발생했습니다: ${e.message}"
                )
            }
        }
    }

    fun clearApiError() {
        uiState = uiState.copy(apiError = null)
    }
}