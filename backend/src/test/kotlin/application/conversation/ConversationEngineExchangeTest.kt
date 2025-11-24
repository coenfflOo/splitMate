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
import kotlin.test.assertFalse

private class FakeProviderOk : ExchangeRateProvider {
    override fun getRate(base: Currency, target: Currency): ExchangeRate {
        return ExchangeRate(base, target, java.math.BigDecimal("1000"))
    }
}
private class FakeProviderFail : ExchangeRateProvider {
    override fun getRate(base: Currency, target: Currency): ExchangeRate {
        throw IllegalStateException("API down")
    }
}

class ConversationEngineExchangeTest {

    @Test
    fun `수동 환율 입력 경로 - KRW 포함 요약 출력`() {
        val engine = ConsoleConversationFlow(ExchangeService(FakeProviderFail()))
        var out = engine.start()

        out = engine.handle(out.nextStep, "27.40", out.context)
        out = engine.handle(out.nextStep, "2.60", out.context)
        out = engine.handle(out.nextStep, "1", out.context)
        out = engine.handle(out.nextStep, "10", out.context)

        out = engine.handle(ConversationStep.ASK_SPLIT_MODE, "1", out.context)

        out = engine.handle(out.nextStep, "2", out.context)
        out = engine.handle(out.nextStep, "2", out.context)
        out = engine.handle(out.nextStep, "1000", out.context)

        assertEquals(ConversationStep.RESTART_CONFIRM, out.nextStep)
        assertFalse(out.isFinished)

        val msg = out.message
        assertTrue(msg.contains("1인당"))
        assertTrue(msg.contains("KRW"))
        assertTrue(msg.contains("환율: 1 CAD = 1,000 KRW"))
    }

    @Test
    fun `자동 환율 조회 경로 - Provider OK`() {
        val engine = ConsoleConversationFlow(ExchangeService(FakeProviderOk()))
        var out = engine.start()

        out = engine.handle(out.nextStep, "10.00", out.context)
        out = engine.handle(out.nextStep, "0", out.context)
        out = engine.handle(out.nextStep, "1", out.context)
        out = engine.handle(out.nextStep, "0", out.context)

        out = engine.handle(ConversationStep.ASK_SPLIT_MODE, "1", out.context)

        out = engine.handle(out.nextStep, "1", out.context)
        out = engine.handle(out.nextStep, "1", out.context)

        assertEquals(ConversationStep.RESTART_CONFIRM, out.nextStep)
        assertFalse(out.isFinished)

        val msg = out.message
        assertTrue(msg.contains("환율: 1 CAD = 1,000 KRW"))
        assertTrue(msg.contains("총 금액: 10.00 CAD"))
        assertTrue(msg.contains("1인당(원화): 10,000.00 KRW"))
    }

    @Test
    fun `자동 환율 조회 실패시 수동 입력으로 폴백 유도`() {
        val engine = ConsoleConversationFlow(ExchangeService(FakeProviderFail()))
        var out = engine.start()

        out = engine.handle(out.nextStep, "10.00", out.context)
        out = engine.handle(out.nextStep, "0", out.context)
        out = engine.handle(out.nextStep, "1", out.context)
        out = engine.handle(out.nextStep, "0", out.context)

        out = engine.handle(ConversationStep.ASK_SPLIT_MODE, "1", out.context)

        out = engine.handle(out.nextStep, "1", out.context)
        out = engine.handle(out.nextStep, "1", out.context)

        assertEquals(ConversationStep.ASK_EXCHANGE_RATE_VALUE, out.nextStep)
        assertTrue(out.message.contains("환율 조회에 실패했습니다"))
    }
}
