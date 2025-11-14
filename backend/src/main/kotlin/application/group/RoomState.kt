package application.group

import domain.conversation.ConversationOutput

data class RoomState(
    val id: RoomId,
    val members: Set<MemberId>,
    val lastOutput: ConversationOutput
)