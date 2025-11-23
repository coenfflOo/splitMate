package application.conversation

import domain.conversation.ConversationOutput
import domain.conversation.ConversationStep
import domain.fx.ExchangeService

class ConversationEngine(
    exchangeService: ExchangeService? = null
) : BaseConversationFlow(exchangeService) {

    override fun start(): ConversationOutput {
        return ConversationOutput(
            nextStep = ConversationStep.ASK_TOTAL_AMOUNT,
            message = "총 결제 금액을 입력해주세요 (예: 27.40)",
            context = ConversationContext()
        )
    }
}
