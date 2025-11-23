package application.conversation.model

data class ConversationOutput(
    val nextStep: ConversationStep,
    val message: String,
    val context: ConversationContext,
    val isFinished: Boolean = false
)