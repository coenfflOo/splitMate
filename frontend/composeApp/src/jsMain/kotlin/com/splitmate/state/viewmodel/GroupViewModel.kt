package com.splitmate.state.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.splitmate.api.dto.GroupCreateRoomRequestDto
import com.splitmate.api.dto.GroupJoinRoomRequestDto
import com.splitmate.api.callCreateRoom
import com.splitmate.api.callJoinRoom
import com.splitmate.state.model.menu.MenuSplitResultUi
import com.splitmate.state.steps.GroupStep
import com.splitmate.state.uistate.GroupUiState
import com.splitmate.websocket.GroupStompClient
import com.splitmate.websocket.dto.WsErrorMessage
import com.splitmate.websocket.dto.WsGroupMessage
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

            if (sender == myId) return

            uiState = uiState.copy(
                members = msg.members.ifEmpty { uiState.members },
                messages = uiState.messages + "$sender: ${msg.message}",
                error = null
            )
            return
        }

        val isMenuStep = newStep in setOf(
            GroupStep.MENU_ITEMS,
            GroupStep.PARTICIPANTS,
            GroupStep.MENU_ASSIGNMENTS
        )

        uiState = uiState.copy(
            joinedRoomId = msg.roomId,
            members = msg.members.ifEmpty { uiState.members },
            currentPrompt = msg.message,
            currentStep = newStep,
            messages = if (!isMenuStep)
                uiState.messages + "서버: ${msg.message}"
            else
                uiState.messages,
            error = null,
            isMenuFlowActive = isMenuStep
        )
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

    fun onSplitModeSelected(mode: String) {
        if (mode == "MENU_BASED") {
            sendPresetInput(mode)
            uiState = uiState.copy(isMenuFlowActive = true)
            return
        }
        sendPresetInput(mode)
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

    fun sendMenuResultAsChat(result: MenuSplitResultUi) {
        val memberId = uiState.memberIdInput.trim()
        if (memberId.isEmpty()) return

        fun fmtCad(v: String) = v  // 필요하면 여기서 소수/콤마 포맷
        fun fmtKrw(v: String?) = v ?: "-"

        val text = buildString {
            appendLine("🍽️ 메뉴별 계산 결과\n")
            appendLine("────────────────────\n")
            appendLine("✅ 총 결제 금액: ${fmtCad(result.totalAmountCad)} CAD")

            result.exchangeMode?.let { mode ->
                val rate = result.exchangeRate ?: "-"
                appendLine("✅ 환율: $mode  (rate: $rate)\n")
            }

            appendLine("────────────────────\n")
            appendLine("👤 인당 부담금\n")

            result.perPersonTotals.forEach { row ->
                val cad = fmtCad(row.totalCad)
                val krw = fmtKrw(row.totalKrw)
                appendLine(" • ${row.participantName}: $cad CAD  /  $krw KRW\n")
            }

            appendLine("────────────────────\n")
        }.trim()

        val safeText = text.replace("\n", "\\n")

        socketClient.sendGroupInput(
            memberId = memberId,
            input = "CHAT:$safeText"
        )

        uiState = uiState.copy(
            messages = uiState.messages + "$memberId: $text"
        )
    }

    fun resetConversation() {
        val memberId = uiState.memberIdInput.trim()
        if (memberId.isEmpty()) return

        socketClient.sendGroupInput(
            memberId = memberId,
            input = "RESET"
        )
    }

    fun backToFirstStep() {
        resetConversation()

        uiState = uiState.copy(
            currentStep = GroupStep.SPLIT_MODE,
            currentPrompt = "분배 방식을 선택하세요",
            isMenuFlowActive = false
        )
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }

}