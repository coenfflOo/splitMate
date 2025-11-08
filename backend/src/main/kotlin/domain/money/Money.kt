package domain.money

import java.math.BigDecimal
import java.math.RoundingMode

class Money(
    val amount: BigDecimal,
    val currency: Currency
) {

    init {
        require(amount >= BigDecimal.ZERO) { "Amount must be >= 0" }
    }

    // 같은 통화끼리만 더할 수 있다.
    operator fun plus(other: Money): Money {
        require(this.currency == other.currency) {
            "Cannot add different currencies: $currency and ${other.currency}"
        }

        val sum = this.amount + other.amount
        // 소수 둘째 자리까지 HALF_UP
        val scaled = sum.setScale(SCALE, ROUNDING_MODE)
        return Money(scaled, currency)
    }

    // N명으로 나누기 (소수 둘째 자리 HALF_UP)
    fun divideBy(divisor: Int): Money {
        require(divisor > 0) { "Divisor must be > 0" }

        val divided = amount
            .setScale(SCALE, ROUNDING_MODE)
            .divide(BigDecimal(divisor), SCALE, ROUNDING_MODE)

        return Money(divided, currency)
    }

    companion object {
        private const val SCALE = 2
        private val ROUNDING_MODE = RoundingMode.HALF_UP
    }
}