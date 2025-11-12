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

    operator fun plus(other: Money): Money {
        require(this.currency == other.currency) {
            "Cannot add different currencies: $currency and ${other.currency}"
        }
        return of(this.amount + other.amount, currency)
    }

    fun divideBy(divisor: Int): Money {
        require(divisor > 0) { "Divisor must be > 0" }

        val divided = amount
            .setScale(SCALE, ROUNDING_MODE)
            .divide(BigDecimal(divisor), SCALE, ROUNDING_MODE)

        return of(divided, currency)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Money) return false
        if (currency != other.currency) return false
        // 10.0 == 10.00 을 같게 보려면 compareTo로 비교
        return amount.compareTo(other.amount) == 0
    }

    override fun hashCode(): Int {
        // 스케일 차이를 줄이기 위해 stripTrailingZeros() 사용
        val norm = amount.stripTrailingZeros()
        return 31 * norm.hashCode() + currency.hashCode()
    }

    override fun toString(): String = "Money(amount=$amount, currency=$currency)"

    companion object {
        private const val SCALE = 2
        private val ROUNDING_MODE = RoundingMode.HALF_UP

        fun of(amount: BigDecimal, currency: Currency): Money {
            val scaled = amount.setScale(SCALE, ROUNDING_MODE)
            return Money(scaled, currency)
        }

        fun of(amount: String, currency: Currency): Money =
            of(BigDecimal(amount), currency)

        fun zero(currency: Currency): Money =
            of(BigDecimal.ZERO, currency)
    }
}
