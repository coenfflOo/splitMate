package domain.receipt

import domain.money.Money
import java.math.BigDecimal

data class Tip(
    val mode: TipMode,
    val percent: Int? = null,
    val absolute: Money? = null
) {

    init {
        if (mode == TipMode.PERCENT) {
            require(percent in 0..100) {
                "Tip percent must be between 0 and 100: $percent"
            }
        }
    }

    fun amountOn(base: Money): Money {
        return when (mode) {
            TipMode.PERCENT -> {
                requireNotNull(percent) { "percent required for PERCENT mode" }
                val ratio = BigDecimal(percent).divide(BigDecimal(100))
                Money.of(base.amount.multiply(ratio), base.currency)
            }

            TipMode.ABSOLUTE -> {
                requireNotNull(absolute) { "absolute required for ABSOLUTE mode" }
                absolute
            }

            TipMode.NONE -> Money.zero(base.currency)
        }
    }
}
