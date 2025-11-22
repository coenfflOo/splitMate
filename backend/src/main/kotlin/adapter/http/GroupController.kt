package adapter.http

import adapter.http.dto.*
import adapter.websocket.toResponse
import application.group.GroupConversationService
import application.group.MemberId
import application.group.RoomId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/group")
class GroupController(
    private val groupService: GroupConversationService
) {

    @PostMapping("/rooms")
    fun createRoom(
        @RequestBody request: GroupCreateRoomRequest
    ): ResponseEntity<GroupRoomResponse> {
        val roomId = RoomId(request.roomId)
        val memberId = MemberId(request.memberId)

        val state = groupService.createRoom(roomId, memberId)

        return ResponseEntity.ok(state.toResponse())
    }

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

    /**
     * 방 상태 조회 API
     *
     * GET /api/group/rooms/{roomId}
     */
    @GetMapping("/rooms/{roomId}")
    fun getRoom(
        @PathVariable roomId: String
    ): ResponseEntity<Any> {
        val id = RoomId(roomId)
        val state = groupService.getRoom(id)

        return if (state != null) {
            ResponseEntity.ok(state.toResponse())
        } else {
            val body = ErrorResponse(
                error = ErrorBody(
                    code = "ROOM_NOT_FOUND",
                    message = "Room not found: $roomId"
                )
            )
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(body)
        }
    }

    // ----------------- private mapper -----------------
}
