package application.conversation

data class ConversationOutput(
    val nextStep: ConversationStep,
    val message: String,
    val context: ConversationContext,
    val isFinished: Boolean = false
)