package application.conversation

import domain.fx.ExchangeRate
import domain.fx.ExchangeRateProvider
import domain.fx.ExchangeService
import domain.money.Currency
import kotlin.test.Test
import kotlin.test.assertTrue

private class FakeProvider1000TaxNone : ExchangeRateProvider {
    override fun getRate(base: Currency, target: Currency): ExchangeRate {
        return ExchangeRate(base, target, java.math.BigDecimal("1000"))
    }
}


class ConversationEngineTaxNoneTest {

    @Test
    fun `세금을 없음으로 입력하면 0으로 처리된다`() {
        val engine = ConversationEngine(ExchangeService(FakeProvider1000TaxNone()))
        var out = engine.start()

        // 총액 -> 세금(없음) -> 팁 없음 -> 분배 1/N -> 인원 1 -> 환율 없이 CAD만 보기
        out = engine.handle(out.nextStep, "27.40", out.context)      // 총액
        out = engine.handle(out.nextStep, "없음", out.context)        // 세금 없음
        out = engine.handle(out.nextStep, "3", out.context)          // 팁 없음
        out = engine.handle(out.nextStep, "1", out.context)          // 분배: 1/N
        out = engine.handle(out.nextStep, "1", out.context)          // 인원: 1
        out = engine.handle(out.nextStep, "3", out.context)          // 환율: CAD만 보기

        val msg = out.message
        // 세금 0, 팁 0 → 총액 = 27.40 CAD
        assertTrue(msg.contains("총 금액: 27.40 CAD"))
        assertTrue(msg.contains("1인당: 27.40 CAD"))
    }
}
