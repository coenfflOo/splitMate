package domain.receipt

import domain.money.Money
import java.math.BigDecimal

class Tip(
    val mode: TipMode,
    val value: BigDecimal
) {
    fun calculate(base: Money): Money {
        throw NotImplementedError("Tip.calculate not implemented yet")
    }
}
