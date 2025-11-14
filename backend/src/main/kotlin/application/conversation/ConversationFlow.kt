// src/main/kotlin/application/conversation/ConversationFlow.kt
package application.conversation

import application.conversation.ConversationContext
import domain.conversation.ConversationOutput
import domain.conversation.ConversationStep

/**
 * GroupConversationService가 의존하는 포트.
 * ConversationEngine이 이 인터페이스를 구현하도록 하면 됨.
 */
interface ConversationFlow {
    fun start(): ConversationOutput

    fun handle(
        step: ConversationStep,
        input: String,
        context: ConversationContext
    ): ConversationOutput
}
