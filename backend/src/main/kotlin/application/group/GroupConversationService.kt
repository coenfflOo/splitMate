package application.group

import application.conversation.model.ConversationContext
import application.conversation.flow.GroupConversationFlow
import application.conversation.model.ConversationOutput
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

@Service
class GroupConversationService(
    private val flow: GroupConversationFlow
) {
    private val rooms: MutableMap<RoomId, RoomState> = ConcurrentHashMap()
    private val log = LoggerFactory.getLogger(javaClass)

    fun createRoom(roomId: RoomId, creator: MemberId): RoomState {
        val existing = rooms[roomId]
        if (existing != null) return existing

        val output: ConversationOutput = flow.start()
        log.info("[createRoom] start nextStep=${output.nextStep}")

        val state = RoomState(
            id = roomId,
            members = setOf(creator),
            lastOutput = output
        )
        rooms[roomId] = state
        return state
    }

    fun joinRoom(roomId: RoomId, memberId: MemberId): RoomState {
        val current = rooms[roomId] ?: throw RoomNotFoundException(roomId)
        if (memberId in current.members) return current

        val updated = current.copy(members = current.members + memberId)
        rooms[roomId] = updated
        return updated
    }

    fun handleMessage(roomId: RoomId, memberId: MemberId, input: String): RoomState {
        val current = rooms[roomId] ?: throw RoomNotFoundException(roomId)
        val trimmed = input.trim()

        if (trimmed.equals("RESET", ignoreCase = true)) {
            val membersSnapshot = current.members

            val output: ConversationOutput = flow.start()
            log.info("[RESET] start nextStep=${output.nextStep}")

            val newState = RoomState(
                id = roomId,
                members = membersSnapshot,
                lastOutput = output
            )

            rooms[roomId] = newState
            return newState
        }

        if (memberId !in current.members) {
            throw IllegalArgumentException("Member ${memberId.value} is not in room ${roomId.value}")
        }

        val last = current.lastOutput
        val context = last.context as? ConversationContext
            ?: throw IllegalStateException("Context missing for room ${roomId.value}")

        val newOutput = flow.handle(
            step = last.nextStep,
            input = input,
            context = context
        )

        val updated = current.copy(lastOutput = newOutput)
        rooms[roomId] = updated
        return updated
    }

    fun getRoom(roomId: RoomId): RoomState? = rooms[roomId]
}
