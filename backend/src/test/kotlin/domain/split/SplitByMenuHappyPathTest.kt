package domain.split

import domain.menu.MenuAssignment
import domain.menu.MenuItem
import domain.menu.Participant
import domain.money.Currency
import domain.money.Money
import domain.receipt.Receipt
import domain.receipt.Tax
import domain.receipt.Tip
import domain.receipt.TipMode
import kotlin.test.Test
import kotlin.test.assertEquals

class SplitByMenuHappyPathTest {

    @Test
    fun `모두가 공유한 메뉴 1개 - 세금과 팁 비례 분배`() {
        // base 30.00, tax 2.60, tip 10% of (30+2.60=32.60) => 3.26, total 35.86
        val base = Money.of("30.00", Currency.CAD)
        val tax = Tax(Money.of("2.60", Currency.CAD))
        val tip = Tip(TipMode.PERCENT, 10)
        val receipt = Receipt(base, tax, tip)

        val pA = Participant("A", "Alice")
        val pB = Participant("B", "Bob")

        val item = MenuItem.ofCad("m1", "Big Plate", "30.00")
        val assignments = listOf(
            MenuAssignment(item, listOf(pA, pB))
        )

        val result = SplitCalculator.splitByMenu(receipt, assignments)

        // 각자 subtotal = 15.00
        // ratio = 15/30 = 0.5
        // taxShare = 2.60 * 0.5 = 1.30
        // tipShare = 3.26 * 0.5 = 1.63  (HALF_UP)
        // total = 15 + 1.30 + 1.63 = 17.93
        assertEquals(Money.of("35.86", Currency.CAD), result.total)

        val shareA = result.shares.first { it.participant.id == "A" }
        val shareB = result.shares.first { it.participant.id == "B" }

        assertEquals(Money.of("15.00", Currency.CAD), shareA.subtotal)
        assertEquals(Money.of("1.30", Currency.CAD), shareA.taxShare)
        assertEquals(Money.of("1.63", Currency.CAD), shareA.tipShare)
        assertEquals(Money.of("17.93", Currency.CAD), shareA.total)

        assertEquals(Money.of("15.00", Currency.CAD), shareB.subtotal)
        assertEquals(Money.of("1.30", Currency.CAD), shareB.taxShare)
        assertEquals(Money.of("1.63", Currency.CAD), shareB.tipShare)
        assertEquals(Money.of("17.93", Currency.CAD), shareB.total)
    }
}
