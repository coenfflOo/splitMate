//package domain.receipt
//
//import domain.money.Money
//import java.math.BigDecimal
//import java.math.RoundingMode
//
//class Tip (
//    val mode: TipMode,
//    val value: BigDecimal
//) {
//
//    init {
//        require(value >= BigDecimal.ZERO) { "Tip value must be >= 0" }
//        if (mode == TipMode.PERCENT) {
//            require(value <= BigDecimal("100")) { "Tip percent must be <= 100" }
//        }
//    }
//
//    fun calculate(base: Money): Money {
//        return when (mode) {
//            TipMode.PERCENT -> calculatePercentTip(base)
//            TipMode.ABSOLUTE -> Money.of(value, base.currency)
//        }
//    }
//
//    private fun calculatePercentTip(base: Money): Money {
//        val percent = value
//            .divide(BigDecimal("100"), SCALE, ROUNDING_MODE)
//        val tipAmount = base.amount
//            .multiply(percent)
//            .setScale(SCALE, ROUNDING_MODE)
//        return Money.of(tipAmount, base.currency)
//    }
//
//    companion object {
//        private const val SCALE = 2
//        private val ROUNDING_MODE = RoundingMode.HALF_UP
//
//        fun percent(value: BigDecimal): Tip =
//            Tip(TipMode.PERCENT, value)
//
//        fun absolute(amount: BigDecimal): Tip =
//            Tip(TipMode.ABSOLUTE, amount)
//    }
//}

package domain.receipt

import domain.money.Money
import java.math.BigDecimal
import java.math.RoundingMode

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
