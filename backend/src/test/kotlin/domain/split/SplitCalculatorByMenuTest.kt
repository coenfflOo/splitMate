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
import kotlin.test.assertSame

class SplitCalculatorByMenuTest {

    @Test
    fun `모두가 하나의 메뉴를 같이 나누어 먹으면 균등 분배된다`() {
        val bella = Participant("Bella")
        val alice = Participant("Alice")

        val menu = MenuItem.ofCad(
            id = "m1",
            name = "Pizza",
            amount = "24.00"
        )

        val assignments = listOf(
            MenuAssignment(
                menuItem = menu,
                participants = listOf(bella, alice)
            )
        )

        val receipt = Receipt(
            baseAmount = menu.price,
            tax = Tax(Money.of("0.00", Currency.CAD)),
            tip = null
        )

        val result = SplitCalculator.splitByMenu(receipt, assignments)

        assertEquals(Money.of("24.00", Currency.CAD).toString(), result.total.toString())

        val bellaShare = result.shares.first { it.participant == bella }
        val aliceShare = result.shares.first { it.participant == alice }

        assertEquals(Money.of("12.00", Currency.CAD).toString(), bellaShare.total.toString())
        assertEquals(Money.of("12.00", Currency.CAD).toString(), aliceShare.total.toString())
    }

    @Test
    fun `메뉴별 소계 비율에 따라 세금과 퍼센트 팁을 비례 분배한다`() {
        val bella = Participant("Bella")
        val alice = Participant("Alice")

        val latte = MenuItem.ofCad("latte", "Latte", "5.00")
        val americano = MenuItem.ofCad("ame", "Americano", "4.00")

        val assignments = listOf(
            MenuAssignment(latte, listOf(bella)),
            MenuAssignment(americano, listOf(alice))
        )

        val base = Money.of("9.00", Currency.CAD)
        val tax = Tax(Money.of("0.90", Currency.CAD))
        val tip = Tip(TipMode.PERCENT, 10) // 10%

        val receipt = Receipt(
            baseAmount = base,
            tax = tax,
            tip = tip
        )

        val result = SplitCalculator.splitByMenu(receipt, assignments)

        assertEquals(Money.of("10.89", Currency.CAD).toString(), result.total.toString())

        val bellaShare = result.shares.first { it.participant == bella }
        val aliceShare = result.shares.first { it.participant == alice }

        // 비율: Bella 5/9, Alice 4/9
        // 기대값은 반올림 정책에 따라 검증
        assertEquals(Money.of("6.05", Currency.CAD).toString(), bellaShare.total.toString())
        assertEquals(Money.of("4.84", Currency.CAD).toString(), aliceShare.total.toString())
    }
}
