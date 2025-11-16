package adapter.websocket

import adapter.http.dto.ErrorBody
import adapter.http.dto.ErrorResponse
import adapter.http.dto.GroupMessageRequest
import adapter.http.dto.GroupRoomResponse
import application.group.GroupConversationService
import application.group.MemberId
import application.group.RoomId
import application.group.RoomNotFoundException
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class GroupWebSocketController(
    private val groupService: GroupConversationService,
    private val messagingTemplate: SimpMessagingTemplate
) {

    @MessageMapping("/group/{roomId}/messages")
    fun handleGroupMessage(
        @DestinationVariable roomId: String,
        @Payload message: GroupMessageRequest
    ) {
        try {
            // 1) 도메인 서비스 호출 (정상 흐름)
            val state = groupService.handleMessage(
                RoomId(roomId),
                MemberId(message.memberId),
                message.input
            )

            // 2) RoomState → GroupRoomResponse 변환
            val response = GroupRoomResponse(
                roomId = state.id.value,
                members = state.members.map { it.value }.sorted(),
                message = state.lastOutput.message,
                nextStep = state.lastOutput.nextStep.name
            )

            // 3) 정상 응답 브로드캐스트
            messagingTemplate.convertAndSend("/topic/group/$roomId", response)
        } catch (e: RoomNotFoundException) {
            sendError(roomId, "ROOM_NOT_FOUND", e.message ?: "Room not found: $roomId")
        } catch (e: IllegalArgumentException) {
            sendError(roomId, "INVALID_INPUT", e.message ?: "Invalid input")
        } catch (e: IllegalStateException) {
            sendError(roomId, "CONTEXT_MISSING", e.message ?: "Invalid conversation state")
        }
    }

    private fun sendError(roomId: String, code: String, message: String) {
        val error = ErrorResponse(
            error = ErrorBody(
                code = code,
                message = message
            )
        )
        messagingTemplate.convertAndSend("/topic/group/$roomId.errors", error)
    }
}
