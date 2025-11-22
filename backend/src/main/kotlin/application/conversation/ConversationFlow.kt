// src/main/kotlin/application/conversation/ConversationFlow.kt
package application.conversation

import application.conversation.ConversationContext
import domain.conversation.ConversationOutput
import domain.conversation.ConversationStep
import org.springframework.stereotype.Component

interface ConversationFlow {
    fun start(): ConversationOutput

    fun handle(
        step: ConversationStep,
        input: String,
        context: ConversationContext
    ): ConversationOutput
}
