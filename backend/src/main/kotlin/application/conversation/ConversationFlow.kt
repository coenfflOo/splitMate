package application.conversation

interface ConversationFlow {
    fun start(): ConversationOutput

    fun handle(
        step: ConversationStep,
        input: String,
        context: ConversationContext
    ): ConversationOutput
}