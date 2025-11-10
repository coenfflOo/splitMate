package domain.menu

import domain.money.Currency
import domain.money.Money
import java.math.BigDecimal

data class MenuItem(
    val id: String,
    val name: String,
    val price: Money      // 보통 CAD
) {
    init {
        require(price.amount >= BigDecimal.ZERO) {
            "Menu price must be >= 0"
        }
    }

    companion object {
        fun ofCad(id: String, name: String, amount: String): MenuItem =
            MenuItem(id, name, Money.of(amount, Currency.CAD))
    }
}
