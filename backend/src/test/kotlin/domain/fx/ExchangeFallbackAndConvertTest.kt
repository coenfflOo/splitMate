// src/test/kotlin/application/conversation/ExchangeFallbackAndConvertTest.kt
package application.conversation

import domain.fx.ExchangeRate
import domain.fx.ExchangeRateProvider
import domain.fx.ExchangeService
import domain.money.Currency
import domain.money.Money
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ExchangeFallbackAndConvertTest {

    @Test
    fun `ExchangeService가 없을 때 자동 환율 선택 시 수동 입력 단계로 전환된다`() {
        // given - exchangeService = null 인 엔진
        val engine = ConsoleConversationFlow(exchangeService = null)
        val ctx = ConversationContext()

        // when
        val out = engine.handle(
            ConversationStep.ASK_EXCHANGE_RATE_MODE,
            "1", // 자동 모드 선택
            ctx
        )

        // then
        assertThat(out.nextStep).isEqualTo(ConversationStep.ASK_EXCHANGE_RATE_VALUE)
        assertThat(out.message).contains("자동 환율 조회를 사용할 수 없습니다")
        val newCtx = out.context as ConversationContext
        assertThat(newCtx.wantKrw).isTrue()
        assertThat(newCtx.manualRate).isNull()
    }

    @Test
    fun `환율 조회 실패 시 수동 입력 단계로 fallback 한다`() {
        // given - 항상 실패하는 provider
        val provider = object : ExchangeRateProvider {
            override fun getRate(base: Currency, target: Currency): ExchangeRate {
                throw RuntimeException("API error")
            }
        }
        val exchangeService = ExchangeService(provider)
        val engine = ConsoleConversationFlow(exchangeService)

        val ctx = ConversationContext()

        // when
        val out = engine.handle(
            ConversationStep.ASK_EXCHANGE_RATE_MODE,
            "1",
            ctx
        )

        // then
        assertThat(out.nextStep).isEqualTo(ConversationStep.ASK_EXCHANGE_RATE_VALUE)
        assertThat(out.message).contains("환율 조회에 실패했습니다")
        val newCtx = out.context as ConversationContext
        assertThat(newCtx.wantKrw).isTrue()
        assertThat(newCtx.manualRate).isNull()
    }

    @Test
    fun `ExchangeService convert는 HALF_UP, scale 2 규칙으로 금액을 변환한다`() {
        val provider = object : ExchangeRateProvider {
            override fun getRate(base: Currency, target: Currency): ExchangeRate {
                return ExchangeRate(
                    base = base,
                    target = target,
                    rate = BigDecimal("1000")
                )
            }
        }
        val exchangeService = ExchangeService(provider)

        // ⬇️ Money.of가 추가 반올림을 하지 않도록 깔끔한 값 사용
        val cad = Money.of(BigDecimal("10.55"), Currency.CAD)
        val rate = provider.getRate(Currency.CAD, Currency.KRW)

        val krw = exchangeService.convert(cad, rate)

        // 10.55 * 1000 = 10550.00
        assertThat(krw.currency).isEqualTo(Currency.KRW)
        assertThat(krw.amount).isEqualTo(BigDecimal("10550.00"))
    }
}
