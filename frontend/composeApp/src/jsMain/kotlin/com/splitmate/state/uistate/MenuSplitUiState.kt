package com.splitmate.state.uistate

import com.splitmate.state.model.menu.MenuItemUi
import com.splitmate.state.model.menu.MenuSplitResultUi
import com.splitmate.state.model.menu.ParticipantUi
import com.splitmate.state.model.solo.SoloExchangeMode
import com.splitmate.state.model.solo.SoloTipMode
import com.splitmate.state.steps.MenuStep

data class MenuSplitUiState(
    val step: MenuStep = MenuStep.MENU_ITEMS,
    val menuItems: List<MenuItemUi> = emptyList(),
    val participants: List<ParticipantUi> = emptyList(),
    val assignments: Map<Int, Set<Int>> = emptyMap(),
    val taxInput: String = "",
    val taxError: String? = null,
    val tipMode: SoloTipMode? = null,
    val tipModeError: String? = null,
    val tipValueInput: String = "",
    val tipValueError: String? = null,
    val exchangeMode: SoloExchangeMode? = null,
    val exchangeModeError: String? = null,
    val exchangeRateInput: String = "",
    val exchangeRateError: String? = null,
    val result: MenuSplitResultUi? = null,
    val isLoading: Boolean = false,
    val apiError: String? = null
)