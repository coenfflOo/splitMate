package domain.receipt

import domain.money.Currency
import domain.money.Money
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class ReceiptTest {

    @Test
    fun `총액과 세금과 퍼센트 팁을 모두 포함한 최종 금액을 계산한다`() {
        val base = Money.of("27.40", Currency.CAD)
        val tax = Tax(Money.of("2.60", Currency.CAD)) // 총 30.00
        val tip = Tip(TipMode.PERCENT, BigDecimal("10")) // 10% of 30.00 => 3.00

        val receipt = Receipt(baseAmount = base, tax = tax, tip = tip)

        val total = receipt.totalWithTip()

        assertEquals(BigDecimal("33.00"), total.amount)
        assertEquals(Currency.CAD, total.currency)
    }

    @Test
    fun `팁이 없으면 총액은 base와 세금만 포함한다`() {
        val base = Money.of("27.40", Currency.CAD)
        val tax = Tax(Money.of("2.60", Currency.CAD))

        val receipt = Receipt(baseAmount = base, tax = tax, tip = null)

        val total = receipt.totalWithTip()

        assertEquals(BigDecimal("30.00"), total.amount)
    }

    @Test
    fun `고정 금액 팁도 포함해서 최종 금액을 계산한다`() {
        val base = Money.of("20.00", Currency.CAD)
        val tax = Tax(Money.of("0.00", Currency.CAD))
        val tip = Tip(TipMode.ABSOLUTE, BigDecimal("5.00"))

        val receipt = Receipt(baseAmount = base, tax = tax, tip = tip)

        val total = receipt.totalWithTip()

        assertEquals(BigDecimal("25.00"), total.amount)
    }
}
