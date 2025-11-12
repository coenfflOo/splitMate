package domain.split

import domain.menu.MenuAssignment
import domain.menu.MenuItem
import domain.menu.Participant
import domain.money.Currency
import domain.money.Money
import domain.receipt.Receipt
import domain.receipt.Tax
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SplitByMenuRoundingTest {

    @Test
    fun `3명이 10_00을 균등 분할하면 각 3_33, subtotal 합은 9_99가 될 수 있다`() {
        val base = Money.of("10.00", Currency.CAD)
        val tax = Tax(Money.zero(Currency.CAD))
        val receipt = Receipt(base, tax, tip = null)

        val a = Participant("A")
        val b = Participant("B")
        val c = Participant("C")

        val item = MenuItem.ofCad("m1", "Salad", "10.00")
        val assignments = listOf(
            MenuAssignment(item, listOf(a, b, c))
        )

        val result = SplitCalculator.splitByMenu(receipt, assignments)

        val sa = result.shares.first { it.participant.id == "A" }
        val sb = result.shares.first { it.participant.id == "B" }
        val sc = result.shares.first { it.participant.id == "C" }

        assertEquals(Money.of("3.33", Currency.CAD), sa.subtotal)
        assertEquals(Money.of("3.33", Currency.CAD), sb.subtotal)
        assertEquals(Money.of("3.33", Currency.CAD), sc.subtotal)

        // 소계 합이 base와 정확히 일치하지 않을 수 있음(9.99 vs 10.00)
        val sum = sa.subtotal.amount + sb.subtotal.amount + sc.subtotal.amount
        assertTrue(sum.toPlainString() == "9.99")
        assertEquals(Money.of("10.00", Currency.CAD), result.total) // 총계는 영수증 기준
    }
}
