package domain.split

import domain.menu.MenuAssignment
import domain.menu.MenuItem
import domain.menu.Participant
import domain.money.Currency
import domain.money.Money
import domain.receipt.Receipt
import domain.receipt.Tax
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class SplitByMenuRoundingAdjustmentTest {

    @Test
    fun `3명이 10달러를 나누면 둘은 3_33 한 명은 3_34로 보정되고 총합은 10_00이 된다`() {
        val currency = Currency.CAD

        // 메뉴 1개: 10.00 CAD
        val menu = MenuItem.ofCad(
            id = "m1",
            name = "Shared Dish",
            amount = "10.00"
        )

        val alice = Participant(id = "A", displayName = "Alice")
        val bob = Participant(id = "B", displayName = "Bob")
        val charlie = Participant(id = "C", displayName = "Charlie")

        val assignment = MenuAssignment(
            menuItem = menu,
            participants = listOf(alice, bob, charlie)
        )

        // base=10.00, tax=0, tip 없음
        val base = Money.of("10.00", currency)
        val tax = Tax(Money.zero(currency))
        val receipt = Receipt(
            baseAmount = base,
            tax = tax,
            tip = null
        )

        val result = SplitCalculator.splitByMenu(
            receipt = receipt,
            assignments = listOf(assignment)
        )

        // 총합 == 10.00
        val sumOfTotals = result.shares
            .map { it.total.amount }
            .fold(BigDecimal.ZERO, BigDecimal::add)

        assertEquals(BigDecimal("10.00"), result.total.amount)
        assertEquals(BigDecimal("10.00"), sumOfTotals)

        val totals = result.shares.map { it.total.amount }

        // 둘은 3.33, 한 명은 3.34
        assertEquals(2, totals.count { it == BigDecimal("3.33") })
        assertEquals(1, totals.count { it == BigDecimal("3.34") })
    }
}
