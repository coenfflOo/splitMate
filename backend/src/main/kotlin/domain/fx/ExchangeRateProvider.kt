package domain.fx

import domain.money.Currency

interface ExchangeRateProvider {
    fun getRate(base: Currency, target: Currency): ExchangeRate
}
