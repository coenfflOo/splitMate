package domain.fx

import domain.money.Currency
import domain.money.Money
import java.math.RoundingMode

class ExchangeService(
    private val provider: ExchangeRateProvider
) {

    fun getCadToKrwRate(): ExchangeRate {
        return provider.getRate(Currency.CAD, Currency.KRW)
    }

    // 주어진 Money를 환율에 따라 target 통화로 변환한다.
    fun convert(money: Money, rate: ExchangeRate): Money {
        require(money.currency == rate.base) {
            "Money currency (${money.currency}) must match rate base (${rate.base})"
        }

        val convertedAmount = money.amount
            .multiply(rate.rate)
            .setScale(SCALE, ROUNDING_MODE)

        return Money.of(convertedAmount, rate.target)
    }

    companion object {
        private const val SCALE = 2
        private val ROUNDING_MODE = RoundingMode.HALF_UP
    }
}
