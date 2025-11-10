package domain.receipt

import domain.money.Currency
import domain.money.Money
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TipTest {

    @Test
    fun `퍼센트 팁은 기준 금액의 퍼센트를 계산한다`() {
        val base = Money.of("100.00", Currency.CAD)
        val tip = Tip(TipMode.PERCENT, 15) // 15%

        val tipAmount = tip.amountOn(base)

        assertEquals(BigDecimal("15.00"), tipAmount.amount)
        assertEquals(Currency.CAD, tipAmount.currency)
    }

    @Test
    fun `고정 금액 팁은 그대로 사용한다`() {
        val base = Money.of("100.00", Currency.CAD)
        val tip = Tip(
            mode = TipMode.ABSOLUTE,
            absolute = Money.of("5.25", Currency.CAD)   // +5.00
        )
        val tipAmount = tip.amountOn(base)

        assertEquals(BigDecimal("5.25"), tipAmount.amount)
    }

    @Test
    fun `퍼센트 팁은 0 이상 100 이하만 허용한다`() {
        assertFailsWith<IllegalArgumentException> {
            Tip(TipMode.PERCENT, -1)
        }
        assertFailsWith<IllegalArgumentException> {
            Tip(TipMode.PERCENT, 101)
        }
    }
}
