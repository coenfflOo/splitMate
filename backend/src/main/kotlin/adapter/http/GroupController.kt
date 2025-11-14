package adapter.http

import adapter.http.dto.*
import application.group.GroupConversationService
import application.group.MemberId
import application.group.RoomId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/group")
class GroupController(
    private val groupService: GroupConversationService
) {

    /**
     * 방 생성
     *
     * POST /api/group/rooms
     */
    @PostMapping("/rooms")
    fun createRoom(
        @RequestBody request: GroupCreateRoomRequest
    ): ResponseEntity<GroupRoomResponse> {
        val roomId = RoomId(request.roomId)
        val memberId = MemberId(request.memberId)

        val state = groupService.createRoom(roomId, memberId)

        return ResponseEntity.ok(state.toResponse())
    }

    /**
     * 방 참가
     *
     * POST /api/group/rooms/{roomId}/join
     */
    @PostMapping("/rooms/{roomId}/join")
    fun joinRoom(
        @PathVariable roomId: String,
        @RequestBody request: GroupJoinRoomRequest
    ): ResponseEntity<GroupRoomResponse> {
        val id = RoomId(roomId)
        val memberId = MemberId(request.memberId)

        val state = groupService.joinRoom(id, memberId)

        return ResponseEntity.ok(state.toResponse())
    }

    /**
     * 메시지 전송 (대화 진행)
     *
     * POST /api/group/rooms/{roomId}/messages
     */
    @PostMapping("/rooms/{roomId}/messages")
    fun handleMessage(
        @PathVariable roomId: String,
        @RequestBody request: GroupMessageRequest
    ): ResponseEntity<GroupRoomResponse> {
        val id = RoomId(roomId)
        val memberId = MemberId(request.memberId)

        val state = groupService.handleMessage(id, memberId, request.input)

        return ResponseEntity.ok(state.toResponse())
    }

    // ----------------- private mapper -----------------

    private fun application.group.RoomState.toResponse(): GroupRoomResponse =
        GroupRoomResponse(
            roomId = this.id.value,
            members = this.members.map { it.value }.sorted(), // 정렬해서 테스트 예측 가능하게
            message = this.lastOutput.message,
            nextStep = this.lastOutput.nextStep.name
        )
}
