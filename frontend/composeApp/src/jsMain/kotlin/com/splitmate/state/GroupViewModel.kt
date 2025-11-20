package com.splitmate.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
class GroupViewModel {

    var uiState by mutableStateOf(GroupUiState())
        private set

    fun onRoomIdChange(input: String) {
        uiState = uiState.copy(
            roomIdInput = input,
            error = null
        )
    }

    fun onMemberIdChange(input: String) {
        uiState = uiState.copy(
            memberIdInput = input,
            error = null
        )
    }

    fun createAndJoinRoom() {
        val roomId = uiState.roomIdInput.trim()
        val memberId = uiState.memberIdInput.trim()

        if (roomId.isEmpty() || memberId.isEmpty()) {
            uiState = uiState.copy(
                error = "Room ID와 Member ID를 모두 입력해주세요."
            )
            return
        }

        uiState = uiState.copy(
            isJoined = true,
            joinedRoomId = roomId,
            members = listOf(memberId),
            messages = emptyList(),
            currentPrompt = "총 결제 금액을 입력해주세요. (서버와 WebSocket 연동은 추후 구현)",
            error = null,
            info = "방이 생성되었다고 가정하고 입장했습니다. (Mock 상태)"
        )
    }

    fun joinExistingRoom() {
        val roomId = uiState.roomIdInput.trim()
        val memberId = uiState.memberIdInput.trim()

        if (roomId.isEmpty() || memberId.isEmpty()) {
            uiState = uiState.copy(
                error = "Room ID와 Member ID를 모두 입력해주세요."
            )
            return
        }

        uiState = uiState.copy(
            isJoined = true,
            joinedRoomId = roomId,
            members = (uiState.members + memberId).distinct(),
            info = "기존 방에 입장했다고 가정합니다. (Mock 상태)",
            error = null
        )
    }

    fun onInputTextChange(input: String) {
        uiState = uiState.copy(inputText = input)
    }


    fun sendMessageMock() {
        val text = uiState.inputText.trim()
        if (text.isEmpty()) return

        val newMessage = "나: $text"
        uiState = uiState.copy(
            messages = uiState.messages + newMessage,
            inputText = ""
        )
    }

    fun clearInfo() {
        uiState = uiState.copy(info = null)
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }
}