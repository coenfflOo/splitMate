package adapter.http

import adapter.http.dto.*
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

@SpringBootTest(classes = [AppConfig::class, SplitControllerByMenuFxAutoTest.FxTestConfig::class])
@AutoConfigureMockMvc
class SplitControllerByMenuFxAutoTest(

    @Autowired
    private val mockMvc: MockMvc,

    @Autowired
    private val objectMapper: ObjectMapper
) {

    @TestConfiguration
    open class FxTestConfig {
        @Bean
        open fun exchangeService(): ExchangeService {
            val provider = object : ExchangeRateProvider {
                override fun getRate(base: Currency, target: Currency): ExchangeRate {
                    // 1 CAD = 1000 KRW 고정
                    return ExchangeRate(
                        base = base,
                        target = target,
                        rate = java.math.BigDecimal("1000")
                    )
                }
            }
            return ExchangeService(provider)
        }
    }

    @Test
    fun `메뉴별 계산 - AUTO 환율로 KRW까지 반환`() {
        // base: 30.00 CAD (18 + 12)
        val items = listOf(
            MenuItemRequest(id = "m1", name = "Pizza", price = "18.00"),
            MenuItemRequest(id = "m2", name = "Pasta", price = "12.00")
        )

        val participants = listOf(
            ParticipantRequest(id = "A", name = "Alice"),
            ParticipantRequest(id = "B", name = "Bob")
        )

        val assignments = listOf(
            // Pizza(18.00) 둘이 나눠 먹음 → 9 + 9
            MenuAssignmentRequest(menuId = "m1", participantIds = listOf("A", "B")),
            // Pasta(12.00) A 혼자 → A subtotal=21, B subtotal=9
            MenuAssignmentRequest(menuId = "m2", participantIds = listOf("A"))
        )

        // tax = 3.00, tip 10% (base+tax=33.00 → tip=3.30)
        // total = 36.30
        // A ratio = 21/30 = 0.7 → tax 2.10, tip 2.31 → A total = 25.41
        // B ratio =  9/30 = 0.3 → tax 0.90, tip 0.99 → B total = 10.89
        // rate = 1000 → A 25410.00 KRW, B 10890.00 KRW
        val request = MenuSplitRequest(
            currency = "CAD",
            items = items,
            participants = participants,
            assignments = assignments,
            taxAmount = "3.00",
            tip = TipRequest(
                mode = "PERCENT",
                percent = 10
            ),
            exchange = ExchangeOptionRequest(
                mode = "AUTO",
                manualRate = null
            )
        )

        val json = objectMapper.writeValueAsString(request)

        mockMvc.post("/api/split/by-menu") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }
            .andExpect {
                status { isOk() }

                jsonPath("$.currency") { value("CAD") }
                jsonPath("$.totalAmountCad") { value("36.30") }

                jsonPath("$.exchange.mode") { value("AUTO") }
                jsonPath("$.exchange.rate") { value("1000") }
                jsonPath("$.exchange.targetCurrency") { value("KRW") }

                // A
                jsonPath("$.participants[0].id") { value("A") }
                jsonPath("$.participants[0].subtotalCad") { value("21.00") }
                jsonPath("$.participants[0].totalCad") { value("25.41") }
                jsonPath("$.participants[0].totalKrw") { value("25410.00") }

                // B
                jsonPath("$.participants[1].id") { value("B") }
                jsonPath("$.participants[1].subtotalCad") { value("9.00") }
                jsonPath("$.participants[1].totalCad") { value("10.89") }
                jsonPath("$.participants[1].totalKrw") { value("10890.00") }
            }
    }
}
