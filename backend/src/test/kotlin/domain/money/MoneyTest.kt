package domain.money

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun `같은 통화끼리 더할 수 있다`() {
        val tenPointFive = Money(BigDecimal("10.50"), Currency.CAD)
        val twoPointTwentyFive = Money(BigDecimal("2.25"), Currency.CAD)

        val result = tenPointFive + twoPointTwentyFive

        assertEquals(BigDecimal("12.75"), result.amount)
        assertEquals(Currency.CAD, result.currency)
    }

    @Test
    fun `다른 통화는 더할 수 없고 예외가 발생한다`() {
        val cad = Money(BigDecimal("10.00"), Currency.CAD)
        val krw = Money(BigDecimal("1000"), Currency.KRW)

        assertFailsWith<IllegalArgumentException> {
            cad + krw
        }
    }

    @Test
    fun `N명으로 나누면 반올림된 금액이 된다`() {
        val total = Money(BigDecimal("10.00"), Currency.CAD)

        val perPerson = total.divideBy(3)

        assertEquals(BigDecimal("3.33"), perPerson.amount)
        assertEquals(Currency.CAD, perPerson.currency)
    }
}
