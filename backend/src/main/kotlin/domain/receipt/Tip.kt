package domain.receipt

import domain.money.Money
import java.math.BigDecimal
import java.math.RoundingMode

class Tip(
    val mode: TipMode,
    val value: BigDecimal
) {

    init {
        require(value >= BigDecimal.ZERO) { "Tip value must be >= 0" }
        if (mode == TipMode.PERCENT) {
            require(value <= BigDecimal("100")) { "Tip percent must be <= 100" }
        }
    }

    fun calculate(base: Money): Money {
        return when (mode) {
            TipMode.PERCENT -> {
                val percent = value
                    .divide(BigDecimal("100"), SCALE, ROUNDING_MODE)
                val tipAmount = base.amount
                    .multiply(percent)
                    .setScale(SCALE, ROUNDING_MODE)

                Money.of(tipAmount, base.currency)
            }
            TipMode.ABSOLUTE -> {
                Money.of(value, base.currency)
            }
        }
    }

    companion object {
        private const val SCALE = 2
        private val ROUNDING_MODE = RoundingMode.HALF_UP
    }
}
