// src/main/kotlin/application/group/GroupConversationService.kt
package application.group

import application.conversation.ConversationContext
import application.conversation.ConversationFlow
import domain.conversation.ConversationOutput
import java.util.concurrent.ConcurrentHashMap

class GroupConversationService(
    private val conversationFlow: ConversationFlow
) {

    private val rooms: MutableMap<RoomId, RoomState> = ConcurrentHashMap()

    fun createRoom(roomId: RoomId, creator: MemberId): RoomState {
        // 이미 존재하면 그대로 반환 (정책은 필요에 따라 변경)
        val existing = rooms[roomId]
        if (existing != null) {
            return existing
        }

        val output: ConversationOutput = conversationFlow.start()
        val state = RoomState(
            id = roomId,
            members = setOf(creator),
            lastOutput = output
        )
        rooms[roomId] = state
        return state
    }

    fun joinRoom(roomId: RoomId, memberId: MemberId): RoomState {
        val current = rooms[roomId]
            ?: throw IllegalArgumentException("Room not found: $roomId")

        // 이미 멤버로 있으면 그대로 반환
        if (memberId in current.members) {
            return current
        }

        val updated = current.copy(
            members = current.members + memberId
        )
        rooms[roomId] = updated
        return updated
    }

    fun handleMessage(roomId: RoomId, memberId: MemberId, input: String): RoomState {
        val current = rooms[roomId]
            ?: throw IllegalArgumentException("Room not found: $roomId")

        // 필요하다면 "member가 방에 없으면 예외" 같은 정책도 추가 가능
        if (memberId !in current.members) {
            // 여기서는 간단히 자동 join 시킬 수도 있고, 예외를 던질 수도 있음
            // 일단 예외로 처리
            throw IllegalArgumentException("Member $memberId is not in room $roomId")
        }

        val last = current.lastOutput
        val context = last.context as? ConversationContext
            ?: throw IllegalStateException("ConversationContext is required in lastOutput")

        val newOutput = conversationFlow.handle(
            step = last.nextStep,
            input = input,
            context = context
        )

        val updated = current.copy(
            lastOutput = newOutput
        )
        rooms[roomId] = updated
        return updated
    }

    fun getRoom(roomId: RoomId): RoomState? = rooms[roomId]
}
