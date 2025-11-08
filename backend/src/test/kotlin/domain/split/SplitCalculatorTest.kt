import domain.money.Currency
import domain.money.Money
import domain.receipt.Receipt
import domain.receipt.Tax
import domain.receipt.Tip
import domain.receipt.TipMode
import domain.split.SplitCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.math.BigDecimal

class SplitCalculatorTest {

    @Test
    fun `N분의 1로 공평하게 나눈다`() {
        // 총액: 27.40 + 2.60 = 30.00
        val base = Money.of("27.40", Currency.CAD)
        val tax = Tax(Money.of("2.60", Currency.CAD))
        val tip = Tip(TipMode.PERCENT, BigDecimal("10"))  // 10% of 30.00 = 3.00

        val receipt = Receipt(base, tax, tip)

        val result = SplitCalculator.splitEvenly(receipt, peopleCount = 3)

        assertEquals(3, result.peopleCount)
        assertEquals(BigDecimal("33.00"), result.total.amount)
        assertEquals(BigDecimal("11.00"), result.perPerson.amount)
    }

    @Test
    fun `인원 수가 1이면 1인당 금액은 총액과 같다`() {
        val base = Money.of("20.00", Currency.CAD)
        val tax = Tax(Money.of("0.00", Currency.CAD))
        val tip = null

        val receipt = Receipt(base, tax, tip)

        val result = SplitCalculator.splitEvenly(receipt, peopleCount = 1)

        assertEquals(BigDecimal("20.00"), result.total.amount)
        assertEquals(BigDecimal("20.00"), result.perPerson.amount)
    }

    @Test
    fun `인원 수가 0 이하이면 예외가 발생한다`() {
        val base = Money.of("10.00", Currency.CAD)
        val tax = Tax(Money.of("0.00", Currency.CAD))
        val receipt = Receipt(base, tax, null)

        assertFailsWith<IllegalArgumentException> {
            SplitCalculator.splitEvenly(receipt, peopleCount = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            SplitCalculator.splitEvenly(receipt, peopleCount = -3)
        }
    }
}
