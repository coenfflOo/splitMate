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
        // TODO: implement
        throw NotImplementedError()
    }

    fun joinRoom(roomId: RoomId, memberId: MemberId): RoomState {
        // TODO: implement
        throw NotImplementedError()
    }

    fun handleMessage(roomId: RoomId, memberId: MemberId, input: String): RoomState {
        // TODO: implement
        throw NotImplementedError()
    }

    fun getRoom(roomId: RoomId): RoomState? = rooms[roomId]

    // 나중에 필요하면 leaveRoom 등 추가 가능
}
