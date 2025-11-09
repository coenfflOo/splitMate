package domain.fx

import domain.money.Currency
import domain.money.Money
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExchangeServiceTest {

    private class FakeExchangeRateProvider : ExchangeRateProvider {
        override fun getRate(base: Currency, target: Currency): ExchangeRate {
            if (base == Currency.CAD && target == Currency.KRW) {
                return ExchangeRate(
                    base = base,
                    target = target,
                    rate = BigDecimal("1000")
                )
            }
            throw IllegalArgumentException("지원하지 않는 통화 조합입니다.")
        }
    }

    private val provider = FakeExchangeRateProvider()
    private val service = ExchangeService(provider)

    @Test
    fun `CAD 금액을 KRW로 변환한다`() {
        val cad = Money.of("10.50", Currency.CAD)   // 10.50 CAD
        val rate = provider.getRate(Currency.CAD, Currency.KRW)

        val krw = service.convert(cad, rate)

        assertEquals(Currency.KRW, krw.currency)
        assertEquals(BigDecimal("10500.00"), krw.amount)
    }

    @Test
    fun `환율의 base 통화와 Money 통화가 다르면 예외가 발생한다`() {
        val krw = Money.of("10000", Currency.KRW)
        val rate = provider.getRate(Currency.CAD, Currency.KRW)

        assertFailsWith<IllegalArgumentException> {
            service.convert(krw, rate)
        }
    }

    @Test
    fun `CAD에서 KRW로 오늘 환율을 조회하는 편의 메서드를 제공한다`() {
        val rate = service.getCadToKrwRate()

        assertEquals(Currency.CAD, rate.base)
        assertEquals(Currency.KRW, rate.target)
        assertEquals(BigDecimal("1000"), rate.rate)
    }
}
