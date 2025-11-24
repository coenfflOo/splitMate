// src/test/kotlin/adapter/http/SplitControllerByMenuFxManualTest.kt
package adapter.http

import adapter.http.dto.*
import application.group.GroupConversationService
import com.fasterxml.jackson.databind.ObjectMapper
import config.AppConfig
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
class SplitControllerByMenuFxTest(

    @Autowired
    private val mockMvc: MockMvc,

    @Autowired
    private val objectMapper: ObjectMapper
) {
    @MockitoBean
    lateinit var exchangeService: ExchangeService

    @MockitoBean
    private lateinit var groupConversationService: GroupConversationService

    @Test
    fun `메뉴별 계산 - MANUAL 환율로 KRW까지 반환`() {
        val items = listOf(
            MenuItemRequest(id = "m1", name = "Pizza", price = "18.00"),
            MenuItemRequest(id = "m2", name = "Pasta", price = "12.00")
        )

        val participants = listOf(
            ParticipantRequest(id = "A", name = "Alice"),
            ParticipantRequest(id = "B", name = "Bob")
        )

        val assignments = listOf(
            MenuAssignmentRequest(menuId = "m1", participantIds = listOf("A", "B")), // 9 + 9
            MenuAssignmentRequest(menuId = "m2", participantIds = listOf("A"))       // +12 → A subtotal=21, B subtotal=9
        )

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
                mode = "MANUAL",
                manualRate = "1000"
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

                jsonPath("$.exchange.mode") { value("MANUAL") }
                jsonPath("$.exchange.rate") { value("1000") }
                jsonPath("$.exchange.targetCurrency") { value("KRW") }

                jsonPath("$.participants[0].id") { value("A") }
                jsonPath("$.participants[0].subtotalCad") { value("21.00") }
                jsonPath("$.participants[0].totalCad") { value("25.41") }
                jsonPath("$.participants[0].totalKrw") { value("25410.00") }

                jsonPath("$.participants[1].id") { value("B") }
                jsonPath("$.participants[1].subtotalCad") { value("9.00") }
                jsonPath("$.participants[1].totalCad") { value("10.89") }
                jsonPath("$.participants[1].totalKrw") { value("10890.00") }
            }
    }
}
