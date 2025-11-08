package domain.money

import java.math.BigDecimal

class Money(
    val amount: BigDecimal,
    val currency: Currency
) {
    operator fun plus(other: Money): Money {
        throw NotImplementedError("plus is not implemented yet")
    }

    fun divideBy(divisor: Int): Money {
        throw NotImplementedError("divideBy is not implemented yet")
    }
}