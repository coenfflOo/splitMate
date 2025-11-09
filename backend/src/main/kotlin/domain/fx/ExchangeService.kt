package domain.fx

import domain.money.Money

class ExchangeService(
    private val provider: ExchangeRateProvider
) {

    fun getCadToKrwRate(): ExchangeRate {
        return provider.getRate(domain.money.Currency.CAD, domain.money.Currency.KRW)
    }

    fun convert(money: Money, rate: ExchangeRate): Money {
        throw NotImplementedError("convert not implemented yet")
    }
}
