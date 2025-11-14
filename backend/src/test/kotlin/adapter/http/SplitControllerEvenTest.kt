package adapter.http

import config.AppConfig
import adapter.http.dto.ExchangeOptionRequest
import adapter.http.dto.SplitEvenRequest
import adapter.http.dto.TipRequest
import application.group.GroupConversationService
import com.fasterxml.jackson.databind.ObjectMapper
import domain.fx.ExchangeService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post


@SpringBootTest(classes = [AppConfig::class])
@AutoConfigureMockMvc
class SplitControllerEvenTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper
) {

    @MockitoBean
    lateinit var exchangeService: ExchangeService

    @MockitoBean
    private lateinit var groupConversationService: GroupConversationService


    @Test
    fun `N분의 1 계산 API - 퍼센트 팁, KRW 변환 없이 행복 경로`() {
        // given
        // 총액 27.40 + 세금 2.60 = 30.00
        // 팁 10% (PERCENT) => 3.00
        // 최종 33.00 → 3명 → 1인당 11.00

        val request = SplitEvenRequest(
            currency = "CAD",
            totalAmount = "27.40",
            taxAmount = "2.60",
            tip = TipRequest(
                mode = "PERCENT",
                percent = 10,
                absolute = null
            ),
            peopleCount = 3,
            exchange = ExchangeOptionRequest(
                mode = "NONE",
                manualRate = null
            )
        )

        val json = objectMapper.writeValueAsString(request)

        // when & then
        mockMvc.post("/api/split/even") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.currency") { value("CAD") }
                jsonPath("$.totalAmountCad") { value("33.00") }
                jsonPath("$.peopleCount") { value(3) }
                jsonPath("$.perPersonCad") { value("11.00") }
                jsonPath("$.exchange") { doesNotExist() }
                jsonPath("$.perPersonKrw") { doesNotExist() }
            }
    }
}
