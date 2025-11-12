package domain.menu

import domain.money.Currency
import domain.money.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MenuDomainTest {

    @Test
    fun `MenuItem 가격은 0 이상이어야 한다`() {
        MenuItem.ofCad(id = "m1", name = "Burger", amount = "0.00") // ok
        assertFailsWith<IllegalArgumentException> {
            MenuItem(id = "m2", name = "Bad", price = Money.of("-1.00", Currency.CAD))
        }
    }

    @Test
    fun `MenuAssignment는 최소 1명 이상의 참가자가 필요하다`() {
        val item = MenuItem.ofCad("m1", "Pasta", "10.00")
        assertFailsWith<IllegalArgumentException> {
            MenuAssignment(menuItem = item, participants = emptyList())
        }
    }

    @Test
    fun `Participant는 id 기준으로 동치 비교된다`() {
        val a1 = Participant(id = "A", displayName = "Alice")
        val a2 = Participant(id = "A", displayName = "ALICE!!")
        assertEquals(a1, a2)
    }
}
