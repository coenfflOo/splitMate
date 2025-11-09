package application.conversation

import domain.fx.ExchangeRate
import domain.fx.ExchangeRateProvider
import domain.fx.ExchangeService
import domain.money.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        val engine = ConversationEngine(ExchangeService(FakeProviderFail())) // 수동 경로만 사용
        var out = engine.start()
        val ctx0 = out.context
        // 총액 -> 세금 -> 팁모드(퍼센트=1) -> 팁값 -> 분배(N=1) -> 인원수 -> 환율모드(수동=2) -> 환율값 -> 요약
        out = engine.handle(out.nextStep, "27.40", ctx0)           // 금액
        out = engine.handle(out.nextStep, "2.60", out.context)     // 세금
        out = engine.handle(out.nextStep, "1", out.context)        // 팁 모드: 퍼센트
        out = engine.handle(out.nextStep, "10", out.context)       // 팁 값: 10%
        out = engine.handle(out.nextStep, "1", out.context)        // 분배 방식: 1/N
        out = engine.handle(out.nextStep, "2", out.context)        // 인원 수: 2
        out = engine.handle(out.nextStep, "2", out.context)        // 환율 모드: 수동
        out = engine.handle(out.nextStep, "1000", out.context)     // 환율 값: 1000

        assertTrue(out.isFinished)
        val msg = out.message
        assertTrue(msg.contains("1인당"), "요약에 1인당 문구 포함")
        assertTrue(msg.contains("KRW"), "KRW 표기가 포함")
        assertTrue(msg.contains("1 CAD = 1,000 KRW"), "환율 표시 포함")
    }

    @Test
    fun `자동 환율 조회 경로 - Provider OK`() {
        val engine = ConversationEngine(ExchangeService(FakeProviderOk()))
        var out = engine.start()
        out = engine.handle(out.nextStep, "10.00", out.context)    // 금액
        out = engine.handle(out.nextStep, "0", out.context)        // 세금
        out = engine.handle(out.nextStep, "1", out.context)        // 팁 모드: 퍼센트
        out = engine.handle(out.nextStep, "0", out.context)        // 팁 값: 0%
        out = engine.handle(out.nextStep, "1", out.context)        // 분배: 1/N
        out = engine.handle(out.nextStep, "1", out.context)        // 인원: 1
        out = engine.handle(out.nextStep, "1", out.context)        // 환율 모드: 자동

        assertTrue(out.isFinished)
        val msg = out.message
        assertTrue(msg.contains("1 CAD = 1,000 KRW"))
        assertTrue(msg.contains("10.00 CAD"))
        assertTrue(msg.contains("10,000.00 KRW"))
    }

    @Test
    fun `자동 환율 조회 실패시 수동 입력으로 폴백 유도`() {
        val engine = ConversationEngine(ExchangeService(FakeProviderFail()))
        var out = engine.start()
        out = engine.handle(out.nextStep, "10.00", out.context)
        out = engine.handle(out.nextStep, "0", out.context)
        out = engine.handle(out.nextStep, "1", out.context)
        out = engine.handle(out.nextStep, "0", out.context)
        out = engine.handle(out.nextStep, "1", out.context)
        out = engine.handle(out.nextStep, "1", out.context)  // 자동 조회 선택

        // 자동 조회 실패 -> 환율 수동 입력 질문 단계로 전환되어야 함
        assertEquals(ConversationStep.ASK_EXCHANGE_RATE_VALUE, out.nextStep)
        assertTrue(out.message.contains("환율 조회에 실패했습니다"))
    }
}
