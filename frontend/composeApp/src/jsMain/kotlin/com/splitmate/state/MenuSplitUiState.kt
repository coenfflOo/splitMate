package com.splitmate.state

data class MenuSplitUiState(
    val step: MenuStep = MenuStep.MENU_ITEMS,
    val menuItems: List<MenuItemUi> = emptyList(),
    val participants: List<ParticipantUi> = emptyList(),
    val assignments: Map<Int, Set<Int>> = emptyMap(),
    val result: MenuSplitResultUi? = null
)