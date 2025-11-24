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
        val base = Money.of("27.40", Currency.CAD)
        val tax = Tax(Money.of("2.60", Currency.CAD))
        val tip = Tip(
            mode = TipMode.PERCENT,
            percent = 10
        )
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

    @Test
    fun `총액, 세금, 퍼센트 팁을 포함해 N분의1을 계산한다`() {
        val base = Money.of(BigDecimal("27.40"), Currency.CAD)
        val tax = Money.of(BigDecimal("2.60"), Currency.CAD)

        val tip = Tip(
            mode = TipMode.PERCENT,
            percent = 10
        )

        val receipt = Receipt(
            baseAmount = base,
            tax = Tax(tax),
            tip = tip
        )

        val result = SplitCalculator.splitEvenly(
            receipt = receipt,
            peopleCount = 2
        )

        assertEquals(result.total,
            Money.of(BigDecimal("33.00"), Currency.CAD)
        )

        assertEquals(result.perPerson,
            Money.of(BigDecimal("16.50"), Currency.CAD)
        )
    }

    @Test
    fun `소수점이 긴 결과는 Money의 반올림 정책을 따른다`() {
        val base = Money.of(BigDecimal("10.00"), Currency.CAD)
        val tax = Money.zero(Currency.CAD)

        val tip = Tip(
            mode = TipMode.ABSOLUTE,
            absolute = Money.of(BigDecimal("1.00"), Currency.CAD)
        )

        val receipt = Receipt(
            baseAmount = base,
            tax = Tax(tax),
            tip = tip
        )

        val result = SplitCalculator.splitEvenly(receipt, 3)

        assertEquals(result.total,
            Money.of(BigDecimal("11.00"), Currency.CAD)
        )
        assertEquals(result.perPerson,
            Money.of(BigDecimal("3.67"), Currency.CAD)
        )
    }
}
