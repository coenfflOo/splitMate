package application.conversation

import application.conversation.flow.ConsoleConversationFlow
import application.conversation.model.ConversationStep
import domain.fx.ExchangeRate
import domain.fx.ExchangeRateProvider
import domain.fx.ExchangeService
import domain.money.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeProvider1000TaxNone : ExchangeRateProvider {
    override fun getRate(base: Currency, target: Currency): ExchangeRate {
        return ExchangeRate(base, target, java.math.BigDecimal("1000"))
    }
}

class ConversationEngineTaxNoneTest {

    @Test
    fun `세금을 없음으로 입력하면 0으로 처리된다`() {
        val engine = ConsoleConversationFlow(ExchangeService(FakeProvider1000TaxNone()))
        var out = engine.start()

        out = engine.handle(out.nextStep, "27.40", out.context)
        assertEquals(ConversationStep.ASK_TAX, out.nextStep)

        out = engine.handle(out.nextStep, "없음", out.context)
        assertEquals(ConversationStep.ASK_TIP_MODE, out.nextStep)

        out = engine.handle(out.nextStep, "3", out.context)
        assertEquals(ConversationStep.ASK_PEOPLE_COUNT, out.nextStep)

        out = engine.handle(out.nextStep, "1", out.context)
        assertEquals(ConversationStep.ASK_EXCHANGE_RATE_MODE, out.nextStep)

        out = engine.handle(out.nextStep, "3", out.context)
        assertEquals(ConversationStep.ASK_SPLIT_MODE, out.nextStep)

        out = engine.handle(out.nextStep, "1", out.context)
        assertEquals(ConversationStep.ASK_PEOPLE_COUNT, out.nextStep)

        out = engine.handle(out.nextStep, "1", out.context)
        assertEquals(ConversationStep.ASK_EXCHANGE_RATE_MODE, out.nextStep)

        out = engine.handle(out.nextStep, "3", out.context)
        assertEquals(ConversationStep.RESTART_CONFIRM, out.nextStep)

        val msg = out.message
        assertTrue(msg.contains("총 금액: 27.40 CAD"))
        assertTrue(msg.contains("1인당: 27.40 CAD"))
    }
}
