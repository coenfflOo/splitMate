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
            ?: throw IllegalArgumentException("Room not found: ${roomId.value}")

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
            ?: throw IllegalArgumentException("Room not found: ${roomId.value}")

        if (memberId !in current.members) {
            throw IllegalArgumentException("Member ${memberId.value} is not in room ${roomId.value}")
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
