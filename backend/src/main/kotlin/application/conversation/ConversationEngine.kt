package application.conversation

import application.session.ConversationContext

class ConversationEngine {

    fun start(): ConversationOutput {
        return ConversationOutput(
            nextStep = ConversationStep.ASK_TOTAL_AMOUNT,
            message = "TODO",
            context = ConversationContext()
        )
    }

    fun handle(
        step: ConversationStep,
        input: String,
        context: ConversationContext
    ): ConversationOutput {
        throw NotImplementedError("handle not implemented yet")
    }
}
