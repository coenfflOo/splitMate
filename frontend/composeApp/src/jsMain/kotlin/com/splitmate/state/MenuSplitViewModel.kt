package com.splitmate.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MenuSplitViewModel {

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

    fun goToResultStep() {
        if (!validateAssignments()) {
            return
        }

        val result = computeLocalResult()
        uiState = uiState.copy(
            step = MenuStep.RESULT,
            result = result
        )
    }


    private fun computeLocalResult(): MenuSplitResultUi {
        // 메뉴 가격 파싱
        val menuPriceMap: Map<Int, Double> = uiState.menuItems.associate { item ->
            val value = item.priceInput.replace(",", "").toDoubleOrNull() ?: 0.0
            item.id to value
        }

        // 참가자별 subtotal 계산
        val subtotalMap = mutableMapOf<Int, Double>()

        uiState.assignments.forEach { (menuId, participantIds) ->
            val price = menuPriceMap[menuId] ?: 0.0
            if (participantIds.isEmpty()) return@forEach

            val share = price / participantIds.size
            participantIds.forEach { pid ->
                val current = subtotalMap[pid] ?: 0.0
                subtotalMap[pid] = current + share
            }
        }

        val perPersonTotals = uiState.participants.map { participant ->
            val subtotal = subtotalMap[participant.id] ?: 0.0
            PerPersonTotalUi(
                participantName = participant.name.ifBlank { "참가자 ${participant.id}" },
                subtotal = subtotal
            )
        }

        val totalAmount = menuPriceMap.values.sum()

        return MenuSplitResultUi(
            perPersonTotals = perPersonTotals,
            totalAmount = totalAmount
        )
    }
}
