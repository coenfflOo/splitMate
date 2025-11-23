package com.splitmate.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.splitmate.api.ExchangeOptionRequestDto
import com.splitmate.api.MenuAssignmentDto
import com.splitmate.api.MenuItemDto
import com.splitmate.api.MenuParticipantDto
import com.splitmate.api.MenuSplitRequestDto
import com.splitmate.api.TipRequestDto
import com.splitmate.api.callMenuSplit
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MenuSplitViewModel {
    private val scope = MainScope()

    var uiState by mutableStateOf(MenuSplitUiState())
        private set

    private var nextMenuId = 1
    private var nextParticipantId = 1

    fun addMenuItem() {
        val newItem = MenuItemUi(
            id = nextMenuId++
        )
        uiState = uiState.copy(
            menuItems = uiState.menuItems + newItem
        )
    }

    fun removeMenuItem(id: Int) {
        val newList = uiState.menuItems.filterNot { it.id == id }
        val newAssignments = uiState.assignments - id
        uiState = uiState.copy(
            menuItems = newList,
            assignments = newAssignments
        )
    }

    fun onMenuNameChange(id: Int, name: String) {
        uiState = uiState.copy(
            menuItems = uiState.menuItems.map {
                if (it.id == id) it.copy(name = name, nameError = null) else it
            }
        )
    }

    fun onMenuPriceChange(id: Int, price: String) {
        uiState = uiState.copy(
            menuItems = uiState.menuItems.map {
                if (it.id == id) it.copy(priceInput = price, priceError = null) else it
            }
        )
    }

    private fun validateMenuItems(): Boolean {
        var valid = true
        val updated = uiState.menuItems.map { item ->
            var nameError: String? = null
            var priceError: String? = null

            if (item.name.isBlank()) {
                nameError = "메뉴 이름을 입력해주세요."
                valid = false
            }

            val numeric = item.priceInput.replace(",", "").trim()
            val value = numeric.toDoubleOrNull()
            if (value == null) {
                priceError = "숫자 형식으로 입력해주세요."
                valid = false
            } else if (value <= 0.0) {
                priceError = "0보다 큰 금액을 입력해주세요."
                valid = false
            }

            item.copy(nameError = nameError, priceError = priceError)
        }

        uiState = uiState.copy(menuItems = updated)
        return valid
    }

    fun goToParticipantsStep() {
        if (uiState.menuItems.isEmpty()) {
            addMenuItem()
            return
        }

        if (!validateMenuItems()) return

        uiState = uiState.copy(step = MenuStep.PARTICIPANTS)
    }

    fun addParticipant() {
        val newParticipant = ParticipantUi(
            id = nextParticipantId++
        )
        uiState = uiState.copy(
            participants = uiState.participants + newParticipant
        )
    }

    fun removeParticipant(id: Int) {
        val newList = uiState.participants.filterNot { it.id == id }
        val newAssignments = uiState.assignments.mapValues { (_, set) ->
            set - id
        }
        uiState = uiState.copy(
            participants = newList,
            assignments = newAssignments
        )
    }

    fun onParticipantNameChange(id: Int, name: String) {
        uiState = uiState.copy(
            participants = uiState.participants.map {
                if (it.id == id) it.copy(name = name, nameError = null) else it
            }
        )
    }

    private fun validateParticipants(): Boolean {
        var valid = true
        val updated = uiState.participants.map { p ->
            val err = if (p.name.isBlank()) {
                valid = false
                "이름을 입력해주세요."
            } else null
            p.copy(nameError = err)
        }

        uiState = uiState.copy(participants = updated)
        return valid
    }

    fun goToAssignmentsStep() {
        if (uiState.participants.isEmpty()) {
            addParticipant()
            return
        }

        if (!validateParticipants()) return

        uiState = uiState.copy(step = MenuStep.ASSIGNMENTS)
    }

    fun backToMenuStep() {
        uiState = uiState.copy(step = MenuStep.MENU_ITEMS)
    }

    fun backToParticipantsStep() {
        uiState = uiState.copy(step = MenuStep.PARTICIPANTS)
    }


    fun toggleAssignment(menuId: Int, participantId: Int) {
        val current = uiState.assignments[menuId] ?: emptySet()
        val newSet = if (current.contains(participantId)) {
            current - participantId
        } else {
            current + participantId
        }
        uiState = uiState.copy(
            assignments = uiState.assignments + (menuId to newSet)
        )
    }

    private fun validateAssignments(): Boolean {
        val menuIds = uiState.menuItems.map { it.id }.toSet()
        val assignments = uiState.assignments

        return menuIds.all { id ->
            (assignments[id] ?: emptySet()).isNotEmpty()
        }
    }

    fun fetchBackendResult() {
        if (uiState.isLoading || uiState.result != null) return

        val request = buildMenuSplitRequestOrNull()
        if (request == null) {
            uiState = uiState.copy(apiError = "입력 값이 유효하지 않습니다.")
            return
        }

        uiState = uiState.copy(isLoading = true, apiError = null)

        scope.launch {
            runCatching {
                callMenuSplit(request)
            }.onSuccess { response ->
                val totals = response.participants.map { p ->
                    PerPersonTotalUi(
                        participantName = p.name,
                        subtotalCad = p.subtotalCad,
                        taxShareCad = p.taxShareCad,
                        tipShareCad = p.tipShareCad,
                        totalCad = p.totalCad,
                        totalKrw = p.totalKrw
                    )
                }

                uiState = uiState.copy(
                    isLoading = false,
                    result = MenuSplitResultUi(
                        totalAmountCad = response.totalAmountCad,
                        exchangeMode = response.exchange?.mode,
                        exchangeRate = response.exchange?.rate,
                        perPersonTotals = totals
                    )
                )
            }.onFailure { e ->
                uiState = uiState.copy(
                    isLoading = false,
                    apiError = "메뉴 계산 요청 중 오류: ${e.message}"
                )
            }
        }
    }

    fun goToTaxStep() {
        if (!validateAssignments()) return
        uiState = uiState.copy(step = MenuStep.TAX)
    }

    fun onTaxChange(input: String) {
        val trimmed = input.trim()
        val error = validateTax(trimmed)
        uiState = uiState.copy(taxInput = input, taxError = error)
    }

    fun onTaxNoneClick() {
        val value = "없음"
        uiState = uiState.copy(taxInput = value, taxError = validateTax(value))
    }

    fun onTaxSubmit(): Boolean {
        val trimmed = uiState.taxInput.trim()
        val error = validateTax(trimmed)
        uiState = uiState.copy(taxError = error)
        if (error != null) return false

        uiState = uiState.copy(step = MenuStep.TIP_MODE)
        return true
    }

    private fun validateTax(input: String): String? {
        if (input.isBlank()) {
            return "세금이 없으면 '없음'을 선택하거나 0을 입력해주세요."
        }
        val normalized = input.lowercase()
        if (normalized in listOf("없음", "none", "no")) return null

        val value = input.replace(",", "").toDoubleOrNull()
            ?: return "숫자 또는 '없음'으로 입력해주세요."
        if (value < 0.0) return "세금 금액은 0 이상이어야 합니다."

        return null
    }

    fun onTipModeSelected(mode: SoloTipMode) {
        uiState = uiState.copy(tipMode = mode, tipModeError = null)
    }

    fun onTipModeProceed() {
        val mode = uiState.tipMode
        if (mode == null) {
            uiState = uiState.copy(tipModeError = "팁 입력 방식을 선택해주세요.")
            return
        }

        uiState = when (mode) {
            SoloTipMode.PERCENT, SoloTipMode.ABSOLUTE ->
                uiState.copy(step = MenuStep.TIP_VALUE)

            SoloTipMode.NONE ->
                uiState.copy(step = MenuStep.EXCHANGE_MODE)
        }
    }

    fun onTipValueChange(input: String) {
        val trimmed = input.trim()
        uiState = uiState.copy(tipValueInput = input, tipValueError = validateTipValue(trimmed))
    }

    fun onTipValueSubmit(): Boolean {
        val trimmed = uiState.tipValueInput.trim()
        val error = validateTipValue(trimmed)
        uiState = uiState.copy(tipValueError = error)
        if (error != null) return false

        uiState = uiState.copy(step = MenuStep.EXCHANGE_MODE)
        return true
    }

    private fun validateTipValue(input: String): String? {
        val mode = uiState.tipMode ?: return "먼저 팁 입력 방식을 선택해주세요."
        if (mode == SoloTipMode.NONE) return null
        if (input.isBlank()) return "팁 값을 입력해주세요."

        val number = input.replace(",", "").toDoubleOrNull()
            ?: return "숫자 형식으로 입력해주세요."

        return when (mode) {
            SoloTipMode.PERCENT ->
                if (number < 0.0 || number > 100.0) "0 ~ 100 사이만 입력 가능" else null

            SoloTipMode.ABSOLUTE ->
                if (number <= 0.0) "0보다 큰 금액을 입력해주세요." else null

            SoloTipMode.NONE -> null
        }
    }

    fun onExchangeModeSelected(mode: SoloExchangeMode) {
        uiState = uiState.copy(exchangeMode = mode, exchangeModeError = null)
    }

    fun onExchangeModeSubmit(): Boolean {
        val mode = uiState.exchangeMode
        if (mode == null) {
            uiState = uiState.copy(exchangeModeError = "환율 모드를 선택해주세요.")
            return false
        }

        uiState = when (mode) {
            SoloExchangeMode.MANUAL -> uiState.copy(step = MenuStep.EXCHANGE_RATE_VALUE)
            SoloExchangeMode.AUTO, SoloExchangeMode.NONE -> uiState.copy(step = MenuStep.RESULT)
        }
        return true
    }

    fun onExchangeRateValueChange(input: String) {
        val trimmed = input.trim()
        uiState = uiState.copy(exchangeRateInput = input, exchangeRateError = validateExchangeRateValue(trimmed))
    }

    fun onExchangeRateValueSubmit(): Boolean {
        val trimmed = uiState.exchangeRateInput.trim()
        val error = validateExchangeRateValue(trimmed)
        uiState = uiState.copy(exchangeRateError = error)
        if (error != null) return false

        uiState = uiState.copy(step = MenuStep.RESULT)
        return true
    }

    private fun validateExchangeRateValue(input: String): String? {
        if (input.isBlank()) return "환율 값을 입력해주세요."
        val value = input.replace(",", "").toDoubleOrNull()
            ?: return "숫자 형식으로 입력해주세요."
        if (value <= 0.0) return "0보다 큰 값을 입력해주세요."
        return null
    }


    fun backToAssignmentsStep() {
        uiState = uiState.copy(step = MenuStep.ASSIGNMENTS)
    }

    fun backToTaxStep() {
        uiState = uiState.copy(step = MenuStep.TAX)
    }

    fun backToTipModeStep() {
        uiState = uiState.copy(step = MenuStep.TIP_MODE)
    }

    fun backToTipValueOrModeStep() {
        uiState = when (uiState.tipMode) {
            SoloTipMode.PERCENT, SoloTipMode.ABSOLUTE -> uiState.copy(step = MenuStep.TIP_VALUE)
            else -> uiState.copy(step = MenuStep.TIP_MODE)
        }
    }

    fun backToExchangeModeStep() {
        uiState = uiState.copy(step = MenuStep.EXCHANGE_MODE)
    }

    private fun buildMenuSplitRequestOrNull(): MenuSplitRequestDto? {
        val s = uiState

        val items = s.menuItems.map { item ->
            val price = item.priceInput.replace(",", "").trim()
            if (item.name.isBlank() || price.toDoubleOrNull() == null) return null
            MenuItemDto(
                id = item.id.toString(),
                name = item.name.trim(),
                price = price
            )
        }

        val participants = s.participants.map { p ->
            if (p.name.isBlank()) return null
            MenuParticipantDto(
                id = p.id.toString(),
                name = p.name.trim()
            )
        }

        val assignments = s.assignments.map { (menuId, pids) ->
            if (pids.isEmpty()) return null
            MenuAssignmentDto(
                menuId = menuId.toString(),
                participantIds = pids.map { it.toString() }
            )
        }

        val taxAmount = when (s.taxInput.trim().lowercase()) {
            "없음", "none", "no" -> "0"
            else -> s.taxInput.replace(",", "").trim()
        }

        val tipModeEnum = s.tipMode ?: SoloTipMode.NONE
        val tipDto = when (tipModeEnum) {
            SoloTipMode.NONE -> TipRequestDto(mode = "NONE")
            SoloTipMode.PERCENT -> TipRequestDto(
                mode = "PERCENT",
                percent = s.tipValueInput.replace(",", "").toDoubleOrNull()?.toInt() ?: return null
            )

            SoloTipMode.ABSOLUTE -> TipRequestDto(
                mode = "ABSOLUTE",
                absolute = s.tipValueInput.replace(",", "").trim()
            )
        }

        val exModeEnum = s.exchangeMode ?: SoloExchangeMode.NONE
        val exchangeDto = when (exModeEnum) {
            SoloExchangeMode.NONE -> ExchangeOptionRequestDto(mode = "NONE")
            SoloExchangeMode.AUTO -> ExchangeOptionRequestDto(mode = "AUTO")
            SoloExchangeMode.MANUAL -> ExchangeOptionRequestDto(
                mode = "MANUAL",
                manualRate = s.exchangeRateInput.replace(",", "").trim()
            )
        }

        return MenuSplitRequestDto(
            items = items,
            participants = participants,
            assignments = assignments,
            taxAmount = taxAmount,
            tip = tipDto,
            exchange = exchangeDto
        )
    }
}