package application.conversation.flow

import application.conversation.model.ConversationContext
import application.conversation.model.ConversationOutput
import application.conversation.model.ConversationStep

interface ConversationFlow {
    fun start(): ConversationOutput

    fun handle(
        step: ConversationStep,
        input: String,
        context: ConversationContext
    ): ConversationOutput
}