package config

import domain.fx.ExchangeRateProvider
import domain.fx.ExchangeService
import adapter.fx.HttpExchangeRateProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FxConfig(
    @Value("\${exchange.api-key}") private val apiKey: String
) {
    @Bean
    fun exchangeRateProvider(): ExchangeRateProvider =
        HttpExchangeRateProvider(authKey = apiKey)

    @Bean
    fun exchangeService(provider: ExchangeRateProvider): ExchangeService =
        ExchangeService(provider)
}
