package domain.fx

import domain.money.Currency
import java.math.BigDecimal

data class ExchangeRate(
    val base: Currency,
    val target: Currency,
    val rate: BigDecimal
)
