package domain.conversation

import application.conversation.ConversationContext

data class ConversationOutput(
    val nextStep: ConversationStep,
    val message: String,
    val context: ConversationContext,
    val isFinished: Boolean = false
)