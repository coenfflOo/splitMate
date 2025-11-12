package adapter.http

import adapter.http.dto.ExchangeOptionRequest
import adapter.http.dto.SplitEvenRequest
import adapter.http.dto.TipRequest
import com.fasterxml.jackson.databind.ObjectMapper
import config.AppConfig
import domain.fx.ExchangeRate
import domain.fx.ExchangeRateProvider
import domain.fx.ExchangeService
import domain.money.Currency
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.math.BigDecimal

@SpringBootTest(
    classes = [
        AppConfig::class,
        SplitControllerEvenExchangeTest.TestConfig::class
    ]
)
@AutoConfigureMockMvc
class SplitControllerEvenExchangeTest(

    @Autowired
    private val mockMvc: MockMvc,

    @Autowired
    private val objectMapper: ObjectMapper
) {

    @TestConfiguration
    class TestConfig {

        @Bean
        fun fakeExchangeRateProvider(): ExchangeRateProvider =
            object : ExchangeRateProvider {
                override fun getRate(base: Currency, target: Currency): ExchangeRate {
                    // 1 CAD = 1000 KRW 라고 가정
                    return ExchangeRate(
                        base = base,
                        target = target,
                        rate = BigDecimal("1000.00")
                    )
                }
            }

        @Bean
        fun exchangeService(provider: ExchangeRateProvider): ExchangeService =
            ExchangeService(provider)
    }

    @Test
    fun `AUTO 환율 모드 - 1인당 CAD를 KRW로 자동 변환`() {
        // 총액 10.00, 세금 0, 팁 없음, 2명
        // 1인당 5.00 CAD → 1 CAD = 1000 KRW → 5,000.00 KRW
        val request = SplitEvenRequest(
            currency = "CAD",
            totalAmount = "10.00",
            taxAmount = "0.00",
            tip = TipRequest(
                mode = "NONE",
                percent = null,
                absolute = null
            ),
            peopleCount = 2,
            exchange = ExchangeOptionRequest(
                mode = "AUTO",
                manualRate = null
            )
        )

        val json = objectMapper.writeValueAsString(request)

        mockMvc.post("/api/split/even") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.currency") { value("CAD") }
                jsonPath("$.totalAmountCad") { value("10.00") }
                jsonPath("$.peopleCount") { value(2) }
                jsonPath("$.perPersonCad") { value("5.00") }

                // 환율 정보
                jsonPath("$.exchange.mode") { value("AUTO") }
                jsonPath("$.exchange.rate") { value("1000.00") }

                // KRW 1인당 금액
                jsonPath("$.perPersonKrw") { value("5000.00") }
            }
    }

    @Test
    fun `MANUAL 환율 모드 - 사용자가 입력한 환율로 1인당 KRW를 계산`() {
        // 총액 12.00, 세금 0, 팁 없음, 3명
        // 1인당 4.00 CAD
        // 환율 900.50 → 4 * 900.50 = 3,602.00 KRW
        val request = SplitEvenRequest(
            currency = "CAD",
            totalAmount = "12.00",
            taxAmount = "0.00",
            tip = TipRequest(
                mode = "NONE",
                percent = null,
                absolute = null
            ),
            peopleCount = 3,
            exchange = ExchangeOptionRequest(
                mode = "MANUAL",
                manualRate = "900.50"
            )
        )

        val json = objectMapper.writeValueAsString(request)

        mockMvc.post("/api/split/even") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.currency") { value("CAD") }
                jsonPath("$.perPersonCad") { value("4.00") }

                jsonPath("$.exchange.mode") { value("MANUAL") }
                jsonPath("$.exchange.rate") { value("900.50") }

                jsonPath("$.perPersonKrw") { value("3602.00") }
            }
    }
}
