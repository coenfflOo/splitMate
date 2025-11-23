package application.conversation

import domain.conversation.ConversationOutput
import domain.conversation.ConversationStep

interface ConversationFlow {
    fun start(): ConversationOutput

    fun handle(
        step: ConversationStep,
        input: String,
        context: ConversationContext
    ): ConversationOutput
}