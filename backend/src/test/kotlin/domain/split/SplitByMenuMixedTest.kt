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

class SplitByMenuMixedTest {

    @Test
    fun `혼자 먹은 메뉴와 공유 메뉴 혼합 - 세금과 팁 비례 분배`() {
        val base = Money.of("30.00", Currency.CAD)
        val tax = Tax(Money.of("3.00", Currency.CAD))
        val tip = Tip(TipMode.PERCENT, 10)
        val receipt = Receipt(base, tax, tip)

        val pA = Participant("A", "Alice")
        val pB = Participant("B", "Bob")

        val soloA = MenuItem.ofCad("m1", "Steak", "12.00")
        val shared = MenuItem.ofCad("m2", "Pizza", "18.00")

        val assignments = listOf(
            MenuAssignment(soloA, listOf(pA)),
            MenuAssignment(shared, listOf(pA, pB))
        )

        val result = SplitCalculator.splitByMenu(receipt, assignments)

        assertEquals(Money.of("36.30", Currency.CAD), result.total)

        val a = result.shares.first { it.participant.id == "A" }
        val b = result.shares.first { it.participant.id == "B" }

        assertEquals(Money.of("21.00", Currency.CAD), a.subtotal)
        assertEquals(Money.of("2.10", Currency.CAD), a.taxShare)
        assertEquals(Money.of("2.31", Currency.CAD), a.tipShare)
        assertEquals(Money.of("25.41", Currency.CAD), a.total)

        assertEquals(Money.of("9.00", Currency.CAD), b.subtotal)
        assertEquals(Money.of("0.90", Currency.CAD), b.taxShare)
        assertEquals(Money.of("0.99", Currency.CAD), b.tipShare)
        assertEquals(Money.of("10.89", Currency.CAD), b.total)
    }
}
