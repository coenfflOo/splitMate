package adapter.http

import domain.fx.ExchangeRateProvider
import domain.fx.ExchangeService
import domain.money.Currency
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import java.math.BigDecimal

@TestConfiguration
class TestExchangeConfig {

    class FakeProvider1000 : ExchangeRateProvider {
        override fun getRate(base: Currency, target: Currency) =
            domain.fx.ExchangeRate(base, target, BigDecimal("1000"))
    }

    @Bean
    fun exchangeRateProvider(): ExchangeRateProvider = FakeProvider1000()

    @Bean
    fun exchangeService(provider: ExchangeRateProvider) = ExchangeService(provider)
}
