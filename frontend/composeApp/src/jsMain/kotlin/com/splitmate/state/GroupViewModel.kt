package com.splitmate.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.splitmate.websocket.GroupStompClient
import com.splitmate.websocket.WsGroupMessage
import com.splitmate.websocket.WsErrorMessage

class GroupViewModel {

    var uiState by mutableStateOf(GroupUiState())
        private set

    private val socketClient = GroupStompClient()

    init {
        socketClient.onConnected = {
            uiState = uiState.copy(
                info = "WebSocket 연결 완료. 서버와 실시간 통신을 시작합니다.",
                error = null
            )
        }

        socketClient.onDisconnected = {
            uiState = uiState.copy(
                info = "WebSocket 연결이 종료되었습니다.",
                isJoined = false,
                joinedRoomId = null
            )
        }

        socketClient.onGroupMessage = { msg: WsGroupMessage ->
            handleGroupMessage(msg)
        }

        socketClient.onErrorMessage = { err: WsErrorMessage ->
            uiState = uiState.copy(
                error = "[${err.code}] ${err.message}"
            )
        }

        socketClient.onConnectionError = { reason: String ->
            uiState = uiState.copy(
                error = reason
            )
        }
    }

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

        // WebSocket 연결 + STOMP 구독
        socketClient.connect(roomId)

        uiState = uiState.copy(
            isJoined = true,
            joinedRoomId = roomId,
            members = listOf(memberId),
            messages = emptyList(),
            currentPrompt = "서버 안내 메시지를 기다리는 중입니다...",
            error = null,
            info = "방이 생성되었다고 가정하고 WebSocket에 연결했습니다."
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

        socketClient.connect(roomId)

        uiState = uiState.copy(
            isJoined = true,
            joinedRoomId = roomId,
            // 단순히 로컬에서 멤버 리스트 업데이트 (추후 REST 응답으로 대체 가능)
            members = (uiState.members + memberId).distinct(),
            messages = emptyList(),
            currentPrompt = "서버 안내 메시지를 기다리는 중입니다...",
            error = null,
            info = "기존 방에 입장했다고 가정하고 WebSocket에 연결했습니다."
        )
    }


    fun onInputTextChange(input: String) {
        uiState = uiState.copy(inputText = input)
    }

    fun sendMessage() {
        val text = uiState.inputText.trim()
        val roomId = uiState.joinedRoomId
        val memberId = uiState.memberIdInput.trim()

        if (text.isEmpty() || roomId.isNullOrEmpty() || memberId.isEmpty()) {
            return
        }

        socketClient.sendGroupInput(
            memberId = memberId,
            input = text
        )

        uiState = uiState.copy(
            messages = uiState.messages + "나: $text",
            inputText = ""
        )
    }

    private fun handleGroupMessage(msg: WsGroupMessage) {
        val newList = uiState.messages + "서버: ${msg.message}"

        uiState = uiState.copy(
            joinedRoomId = msg.roomId,
            members = if (msg.members.isNotEmpty()) msg.members else uiState.members,
            messages = newList,
            currentPrompt = msg.message,
            error = null
        )
    }

    fun clearInfo() {
        uiState = uiState.copy(info = null)
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }

    fun disconnect() {
        socketClient.disconnect()
    }
}