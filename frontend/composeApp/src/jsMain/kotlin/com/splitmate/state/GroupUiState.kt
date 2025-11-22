package com.splitmate.state

data class GroupUiState(
    val roomIdInput: String = "",
    val memberIdInput: String = "",

    val isJoined: Boolean = false,
    val joinedRoomId: String? = null,
    val members: List<String> = emptyList(),
    val messages: List<String> = emptyList(),

    val currentPrompt: String = "",
    val currentStep: GroupStep = GroupStep.UNKNOWN,

    val inputText: String = "",

    val isMenuFlowActive: Boolean = false,

    val error: String? = null,
    val info: String? = null
)