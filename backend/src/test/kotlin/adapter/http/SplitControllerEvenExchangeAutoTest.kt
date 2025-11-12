package adapter.http

import adapter.http.dto.ExchangeOptionRequest
import adapter.http.dto.SplitEvenRequest
import adapter.http.dto.TipRequest
import com.fasterxml.jackson.databind.ObjectMapper
import config.AppConfig
import domain.fx.ExchangeRate
import domain.fx.ExchangeService
import domain.money.Currency
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.math.BigDecimal

@SpringBootTest(classes = [AppConfig::class])
@AutoConfigureMockMvc
class SplitControllerEvenExchangeAutoTest(

    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper
) {

    @MockitoBean
    lateinit var exchangeService: ExchangeService

    @Test
    fun `N분의 1 - AUTO 환율로 KRW 변환 포함`() {
        given(exchangeService.getCadToKrwRate())
            .willReturn(ExchangeRate(Currency.CAD, Currency.KRW, BigDecimal("1000")))

        val req = SplitEvenRequest(
            currency = "CAD",
            totalAmount = "27.40",
            taxAmount = "2.60",
            tip = TipRequest(mode = "PERCENT", percent = 10),
            peopleCount = 3,
            exchange = ExchangeOptionRequest(mode = "AUTO")
        )
        val json = objectMapper.writeValueAsString(req)

        mockMvc.post("/api/split/even") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            jsonPath("$.exchange.mode") { value("AUTO") }
            jsonPath("$.exchange.rate") { value("1000") }
            jsonPath("$.perPersonKrw") { value("11000.00") }
        }
    }

    @Test
    fun `AUTO 환율 실패 시 502와 에러 본문 반환`() {
        given(exchangeService.getCadToKrwRate())
            .willAnswer { throw RuntimeException("remote failed") }

        val req = SplitEvenRequest(
            currency = "CAD",
            totalAmount = "10.00",
            taxAmount = "0",
            tip = TipRequest(mode = "NONE"),
            peopleCount = 1,
            exchange = ExchangeOptionRequest(mode = "AUTO")
        )
        val json = objectMapper.writeValueAsString(req)

        mockMvc.post("/api/split/even") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isBadGateway() }
            jsonPath("$.error.code") { value("EXCHANGE_UNAVAILABLE") }
        }
    }

}
