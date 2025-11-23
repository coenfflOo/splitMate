package com.splitmate.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.splitmate.api.*
import com.splitmate.websocket.GroupStompClient
import com.splitmate.websocket.WsGroupMessage
import com.splitmate.websocket.WsErrorMessage
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class GroupViewModel {

    var uiState by mutableStateOf(GroupUiState())
        private set

    private val scope = MainScope()
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
            uiState = uiState.copy(error = reason)
        }
    }

    fun onRoomIdChange(input: String) {
        uiState = uiState.copy(roomIdInput = input, error = null)
    }

    fun onMemberIdChange(input: String) {
        uiState = uiState.copy(memberIdInput = input, error = null)
    }

    fun createAndJoinRoom() {
        val roomId = uiState.roomIdInput.trim()
        val memberId = uiState.memberIdInput.trim()

        if (roomId.isEmpty() || memberId.isEmpty()) {
            uiState = uiState.copy(error = "Room ID와 Member ID를 모두 입력해주세요.")
            return
        }

        scope.launch {
            uiState = uiState.copy(
                error = null,
                info = "방 생성 중..."
            )

            runCatching {
                callCreateRoom(
                    GroupCreateRoomRequestDto(
                        roomId = roomId,
                        memberId = memberId
                    )
                )
            }.onSuccess { resp ->
                socketClient.connect(resp.roomId)

                uiState = uiState.copy(
                    isJoined = true,
                    joinedRoomId = resp.roomId,
                    members = resp.members,
                    messages = listOf("서버: ${resp.message}"),
                    currentPrompt = resp.message,
                    currentStep = GroupStep.fromServer(resp.nextStep),
                    info = "방 생성 완료. WebSocket 연결을 시작합니다.",
                    error = null
                )

            }.onFailure { e ->
                uiState = uiState.copy(
                    error = "방 생성 실패: ${e.message}",
                    info = null
                )
            }
        }
    }

    fun joinExistingRoom() {
        val roomId = uiState.roomIdInput.trim()
        val memberId = uiState.memberIdInput.trim()

        if (roomId.isEmpty() || memberId.isEmpty()) {
            uiState = uiState.copy(error = "Room ID와 Member ID를 모두 입력해주세요.")
            return
        }

        scope.launch {
            uiState = uiState.copy(
                error = null,
                info = "방 입장 중..."
            )

            runCatching {
                callJoinRoom(
                    roomId = roomId,
                    req = GroupJoinRoomRequestDto(memberId)
                )
            }.onSuccess { resp ->
                socketClient.connect(resp.roomId)

                uiState = uiState.copy(
                    isJoined = true,
                    joinedRoomId = resp.roomId,
                    members = resp.members,
                    messages = emptyList(),
                    currentPrompt = resp.message,
                    currentStep = GroupStep.fromServer(resp.nextStep),
                    info = "입장 완료. WebSocket 연결을 시작합니다.",
                    error = null
                )
            }.onFailure { e ->
                uiState = uiState.copy(
                    error = "방 입장 실패: ${e.message}",
                    info = null
                )
            }
        }
    }

    fun onInputTextChange(input: String) {
        uiState = uiState.copy(inputText = input)
    }

    fun sendMessage(textOverride: String? = null) {
        val text = textOverride ?: uiState.inputText.trim()
        val roomId = uiState.joinedRoomId
        val memberId = uiState.memberIdInput.trim()

        if (text.isEmpty() || roomId.isNullOrEmpty() || memberId.isEmpty()) return

        socketClient.sendGroupInput(
            memberId = memberId,
            input = text
        )

        uiState = uiState.copy(inputText = "")
    }

    private fun handleGroupMessage(msg: WsGroupMessage) {
        val newStep = GroupStep.fromServer(msg.nextStep)
        val isChat = msg.messageType.uppercase() == "CHAT"
        val myId = uiState.memberIdInput.trim()

        if (isChat) {
            val sender = msg.senderId ?: "상대"

            // ✅ 내가 보낸 채팅의 서버 echo는 무시 (로컬에서 이미 찍었음)
            if (sender == myId) return

            uiState = uiState.copy(
                members = if (msg.members.isNotEmpty()) msg.members else uiState.members,
                messages = uiState.messages + "$sender: ${msg.message}",
                error = null
            )
            return
        }

        val shouldKeepInHistory =
            newStep == GroupStep.RESULT || newStep == GroupStep.RESTART_CONFIRM

        uiState = uiState.copy(
            joinedRoomId = msg.roomId,
            members = if (msg.members.isNotEmpty()) msg.members else uiState.members,
            currentPrompt = msg.message,
            currentStep = newStep,
            messages = if (shouldKeepInHistory)
                uiState.messages + "서버: ${msg.message}"
            else
                uiState.messages,
            error = null,
            isMenuFlowActive = false
        )
    }

    fun startMenuFlow() {
        uiState = uiState.copy(isMenuFlowActive = true)
    }

    fun onRestartAnswer(answer: String) {
        if (answer.uppercase() == "Y") {
            uiState = uiState.copy(isMenuFlowActive = false)
        }
        sendPresetInput(answer)
    }

    fun disconnect() {
        socketClient.disconnect()
    }

    fun onTipModeSelected(mode: String) {
        sendPresetInput(mode) // "PERCENT" / "ABSOLUTE" / "NONE"
    }

    fun onSplitModeSelected(mode: String) {
        if (mode == "MENU_BASED") {
            sendPresetInput(mode)
            uiState = uiState.copy(isMenuFlowActive = true)
            return
        }
        sendPresetInput(mode)
    }

    fun exitMenuFlow() {
        uiState = uiState.copy(isMenuFlowActive = false)
    }


    fun onExchangeModeSelected(mode: String) {
        sendPresetInput(mode) // "AUTO" / "MANUAL" / "NONE"
    }

    private fun sendPresetInput(value: String) {
        val memberId = uiState.memberIdInput.trim()
        if (memberId.isEmpty()) return

        socketClient.sendGroupInput(
            memberId = memberId,
            input = value
        )

        uiState = uiState.copy(inputText = "")
    }

    fun onChatTextChange(input: String) {
        uiState = uiState.copy(chatText = input)
    }

    fun sendChat() {
        val text = uiState.chatText.trim()
        val memberId = uiState.memberIdInput.trim()
        if (text.isEmpty() || memberId.isEmpty()) return

        val payload = "CHAT:$text"
        socketClient.sendGroupInput(memberId, payload)

        uiState = uiState.copy(
            messages = uiState.messages + "$memberId: $text",
            chatText = ""
        )
    }

    fun sendMenuPayload(payload: String) {
        val memberId = uiState.memberIdInput.trim()
        val roomId = uiState.joinedRoomId
        if (memberId.isEmpty() || roomId.isNullOrEmpty()) return

        socketClient.sendGroupInput(memberId, payload)

        // ✅ payload 문자열은 히스토리에 안 남기고, 깔끔한 시스템 라인만 남김
        uiState = uiState.copy(
            messages = uiState.messages + "서버: 메뉴별 결과를 전송했습니다. 다음 단계로 진행합니다.",
            isMenuFlowActive = false
        )
    }

    fun sendSystemInput(text: String) {
        val roomId = uiState.joinedRoomId
        val memberId = uiState.memberIdInput.trim()

        if (text.isEmpty() || roomId.isNullOrEmpty() || memberId.isEmpty()) return

        socketClient.sendGroupInput(
            memberId = memberId,
            input = text
        )
    }

}