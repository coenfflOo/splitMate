package adapter.websocket

import adapter.http.dto.GroupMessageRequest
import adapter.http.dto.GroupRoomResponse
import application.group.GroupConversationService
import application.group.MemberId
import application.group.RoomId
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class GroupWebSocketController(
    private val groupService: GroupConversationService,
    private val messagingTemplate: SimpMessagingTemplate
) {

    /**
     * 클라이언트 → 서버:
     *   /app/group/{roomId}/messages
     *
     * 서버 → 클라이언트:
     *   /topic/group/{roomId}
     *
     * HTTP GROUP API와 동일하게 GroupConversationService를 사용하고,
     * 응답 형식도 GroupRoomResponse를 재사용한다.
     */
//    @MessageMapping("/group/{roomId}/messages")
//    @SendTo("/topic/group/{roomId}")
//    fun handleMessage(
//        @DestinationVariable roomId: String,
//        @Payload request: GroupMessageRequest
//    ): GroupRoomResponse {
//        val state = groupService.handleMessage(
//            roomId = RoomId(roomId),
//            memberId = MemberId(request.memberId),
//            input = request.input
//        )
//
//        return GroupRoomResponse(
//            roomId = state.id.value,
//            members = state.members.map { it.value }.sorted(),
//            message = state.lastOutput.message,
//            nextStep = state.lastOutput.nextStep.name
//        )
//
//    }
    @MessageMapping("/group/{roomId}/messages")
    fun handleGroupMessage(
        @DestinationVariable roomId: String,
        message: GroupMessageRequest
    ) {
        // 1) 도메인 서비스 호출
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

        // 3) /topic/group/{roomId} 로 브로드캐스트
        messagingTemplate.convertAndSend("/topic/group/$roomId", response)
    }
}
