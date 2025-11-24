package adapter.http

import application.group.GroupConversationService
import application.group.MemberId
import application.group.RoomId
import application.group.RoomState
import com.fasterxml.jackson.databind.ObjectMapper
import config.AppConfig
import application.conversation.model.ConversationContext
import application.conversation.model.ConversationOutput
import application.conversation.model.ConversationStep
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest(classes = [AppConfig::class])
@AutoConfigureMockMvc
class GroupControllerGetRoomTest(

    @Autowired
    private val mockMvc: MockMvc,

    @Autowired
    private val objectMapper: ObjectMapper
) {

    @MockitoBean
    lateinit var groupConversationService: GroupConversationService

    @Test
    fun `기존 방 상태를 조회하면 200과 room snapshot JSON을 반환한다`() {
        // given
        val roomId = RoomId("room-1")
        val memberA = MemberId("alice")
        val memberB = MemberId("bob")

        val output = ConversationOutput(
            message = "총 결제 금액을 입력해주세요.",
            nextStep = ConversationStep.ASK_TOTAL_AMOUNT,
            context = ConversationContext()
        )

        val state = RoomState(
            id = roomId,
            members = setOf(memberA, memberB),
            lastOutput = output
        )

        given(groupConversationService.getRoom(roomId)).willReturn(state)

        mockMvc.get("/api/group/rooms/${roomId.value}") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.roomId") { value("room-1") }
            jsonPath("$.members[0]") { value("alice") }
            jsonPath("$.members[1]") { value("bob") }
            jsonPath("$.message") { value("총 결제 금액을 입력해주세요.") }
            jsonPath("$.nextStep") { value("ASK_TOTAL_AMOUNT") }
        }
    }

    @Test
    fun `없는 방을 조회하면 404와 ROOM_NOT_FOUND 에러를 반환한다`() {
        val roomId = RoomId("room-404")
        given(groupConversationService.getRoom(roomId)).willReturn(null)

        mockMvc.get("/api/group/rooms/${roomId.value}") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error.code") { value("ROOM_NOT_FOUND") }
            jsonPath("$.error.message") { value("Room not found: room-404") }
        }
    }
}
