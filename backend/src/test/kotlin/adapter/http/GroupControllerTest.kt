// src/test/kotlin/adapter/http/GroupControllerTest.kt
package adapter.http

import adapter.http.dto.GroupDto
import adapter.http.dto.GroupJoinRoomRequest
import adapter.http.dto.GroupMessageRequest
import application.conversation.ConversationContext
import application.group.GroupConversationService
import application.group.MemberId
import application.group.RoomId
import application.group.RoomState
import com.fasterxml.jackson.databind.ObjectMapper
import config.AppConfig
import application.conversation.ConversationOutput
import application.conversation.ConversationStep
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest(classes = [AppConfig::class])
@AutoConfigureMockMvc
class GroupControllerTest(

    @Autowired
    private val mockMvc: MockMvc,

    @Autowired
    private val objectMapper: ObjectMapper
) {

    @MockitoBean
    private lateinit var groupService: GroupConversationService

    @Test
    fun `POST 방 생성 - RoomState를 JSON으로 반환한다`() {
        // given
        val req = GroupDto(
            roomId = "room-1",
            memberId = "alice"
        )
        val ctx = ConversationContext()
        val output = ConversationOutput(
            nextStep = ConversationStep.ASK_TOTAL_AMOUNT,
            message = "총 결제 금액을 입력해주세요",
            context = ctx
        )
        val state = RoomState(
            id = RoomId("room-1"),
            members = setOf(MemberId("alice")),
            lastOutput = output
        )

        given(groupService.createRoom(RoomId("room-1"), MemberId("alice")))
            .willReturn(state)

        val json = objectMapper.writeValueAsString(req)

        // when & then
        mockMvc.post("/api/group/rooms") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            jsonPath("$.roomId") { value("room-1") }
            jsonPath("$.members[0]") { value("alice") }
            jsonPath("$.message") { value("총 결제 금액을 입력해주세요") }
            jsonPath("$.nextStep") { value("ASK_TOTAL_AMOUNT") }
        }
    }

    @Test
    fun `POST 방 참가 - 기존 상태에 멤버만 추가된 모습을 반환한다`() {
        val req = GroupJoinRoomRequest(
            memberId = "bob"
        )

        val ctx = ConversationContext()
        val output = ConversationOutput(
            nextStep = ConversationStep.ASK_TAX,
            message = "세금 금액을 입력해주세요",
            context = ctx
        )

        val state = RoomState(
            id = RoomId("room-1"),
            members = setOf(MemberId("alice"), MemberId("bob")),
            lastOutput = output
        )

        given(groupService.joinRoom(RoomId("room-1"), MemberId("bob")))
            .willReturn(state)

        val json = objectMapper.writeValueAsString(req)

        mockMvc.post("/api/group/rooms/room-1/join") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            jsonPath("$.roomId") { value("room-1") }
            jsonPath("$.members[0]") { value("alice") }
            jsonPath("$.members[1]") { value("bob") }
            jsonPath("$.message") { value("세금 금액을 입력해주세요") }
            jsonPath("$.nextStep") { value("ASK_TAX") }
        }
    }

    @Test
    fun `POST 메시지 - handleMessage 결과 RoomState를 JSON으로 반환한다`() {
        val req = GroupMessageRequest(
            memberId = "alice",
            input = "27.40"
        )

        val ctx = ConversationContext()
        val output = ConversationOutput(
            nextStep = ConversationStep.ASK_TAX,
            message = "세금 금액을 입력해주세요",
            context = ctx
        )

        val state = RoomState(
            id = RoomId("room-1"),
            members = setOf(MemberId("alice")),
            lastOutput = output
        )

        given(groupService.handleMessage(RoomId("room-1"), MemberId("alice"), "27.40"))
            .willReturn(state)

        val json = objectMapper.writeValueAsString(req)

        mockMvc.post("/api/group/rooms/room-1/messages") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            jsonPath("$.roomId") { value("room-1") }
            jsonPath("$.members[0]") { value("alice") }
            jsonPath("$.message") { value("세금 금액을 입력해주세요") }
            jsonPath("$.nextStep") { value("ASK_TAX") }
        }
    }
}
