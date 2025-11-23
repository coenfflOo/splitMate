package application.conversation

import application.conversation.flow.ConsoleConversationFlow
import domain.fx.ExchangeRate
import domain.fx.ExchangeRateProvider
import domain.fx.ExchangeService
import domain.money.Currency
import kotlin.test.Test
import kotlin.test.assertTrue

private class FakeProvider1000 : ExchangeRateProvider {
    override fun getRate(base: Currency, target: Currency): ExchangeRate {
        return ExchangeRate(base, target, java.math.BigDecimal("1000"))
    }
}

class ConversationEngineTipAbsoluteTest {

    @Test
    fun `팁 금액 모드로 10달러 입력 - KRW 자동 환산 포함 요약`() {
        val engine = ConsoleConversationFlow(ExchangeService(FakeProvider1000()))
        var out = engine.start()

        out = engine.handle(out.nextStep, "27.40", out.context)  // 총액
        out = engine.handle(out.nextStep, "2.60", out.context)   // 세금
        out = engine.handle(out.nextStep, "2", out.context)      // 팁 모드: $ 금액
        out = engine.handle(out.nextStep, "10.00", out.context)  // 팁 금액: $10
        out = engine.handle(out.nextStep, "1", out.context)      // 분배: 1/N
        out = engine.handle(out.nextStep, "2", out.context)      // 인원: 2
        out = engine.handle(out.nextStep, "1", out.context)      // 환율: 자동

        val msg = out.message

        // 총액: 27.40 + 2.60 + 10.00 = 40.00 CAD
        assertTrue(msg.contains("총 금액: 40.00 CAD"))
        // 1인당: 20.00 CAD
        assertTrue(msg.contains("1인당: 20.00 CAD"))
        // 환율
        assertTrue(msg.contains("1 CAD = 1,000 KRW"))
        // 1인당 원화
        assertTrue(msg.contains("20,000.00 KRW"))
    }

    @Test
    fun `팁 없음 모드(3) 선택 시 팁 0원 처리`() {
        val engine = ConsoleConversationFlow(ExchangeService(FakeProvider1000()))
        var out = engine.start()
        out = engine.handle(out.nextStep, "10.00", out.context)  // 총액
        out = engine.handle(out.nextStep, "0", out.context)      // 세금
        out = engine.handle(out.nextStep, "3", out.context)      // 팁 없음
        out = engine.handle(out.nextStep, "1", out.context)      // 1/N
        out = engine.handle(out.nextStep, "1", out.context)      // 인원
        out = engine.handle(out.nextStep, "1", out.context)      // 환율 자동

        val msg = out.message
        assertTrue(msg.contains("총 금액: 10.00 CAD"))
        assertTrue(msg.contains("1인당: 10.00 CAD"))
        assertTrue(msg.contains("10,000.00 KRW"))
    }
}
