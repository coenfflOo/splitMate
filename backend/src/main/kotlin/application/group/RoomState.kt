package application.group

import application.conversation.model.ConversationOutput

data class RoomState(
    val id: RoomId,
    val members: Set<MemberId>,
    val lastOutput: ConversationOutput
)