package adapter.http

import adapter.http.dto.ExchangeOptionRequest
import adapter.http.dto.SplitEvenRequest
import adapter.http.dto.TipRequest
import com.fasterxml.jackson.databind.ObjectMapper
import config.AppConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest(classes = [AppConfig::class])
@AutoConfigureMockMvc
class SplitControllerEvenExchangeTest(

    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper
) {

    @Test
    fun `N분의 1 - MANUAL 환율로 KRW 변환 포함`() {
        // 총액 27.40 + 세금 2.60 = 30.00
        // 팁 10% => 3.00 → 총 33.00 → 3명 → 1인당 11.00 CAD
        // manualRate = 1000 → 11,000.00 KRW
        val req = SplitEvenRequest(
            currency = "CAD",
            totalAmount = "27.40",
            taxAmount = "2.60",
            tip = TipRequest(mode = "PERCENT", percent = 10),
            peopleCount = 3,
            exchange = ExchangeOptionRequest(mode = "MANUAL", manualRate = "1000")
        )
        val json = objectMapper.writeValueAsString(req)

        mockMvc.post("/api/split/even") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            jsonPath("$.currency") { value("CAD") }
            jsonPath("$.totalAmountCad") { value("33.00") }
            jsonPath("$.perPersonCad") { value("11.00") }
            jsonPath("$.exchange.mode") { value("MANUAL") }
            jsonPath("$.exchange.rate") { value("1000") }
            jsonPath("$.exchange.targetCurrency") { value("KRW") }
            jsonPath("$.perPersonKrw") { value("11000.00") }
        }
    }

    @Test
    fun `N분의 1 - MANUAL인데 manualRate 누락 시 400 반환`() {
        val req = SplitEvenRequest(
            currency = "CAD",
            totalAmount = "10.00",
            taxAmount = "0",
            tip = TipRequest(mode = "NONE"),
            peopleCount = 1,
            exchange = ExchangeOptionRequest(mode = "MANUAL", manualRate = null)
        )
        val json = objectMapper.writeValueAsString(req)

        mockMvc.post("/api/split/even") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("INVALID_INPUT") }
        }
    }
}
