package adapter.http

import adapter.http.dto.GroupJoinRoomRequest
import adapter.http.dto.GroupMessageRequest
import application.group.GroupConversationService
import application.group.MemberId
import application.group.RoomId
import com.fasterxml.jackson.databind.ObjectMapper
import config.AppConfig
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
class GroupControllerErrorTest(

    @Autowired
    private val mockMvc: MockMvc,

    @Autowired
    private val objectMapper: ObjectMapper
) {

    @MockitoBean
    lateinit var groupConversationService: GroupConversationService

    @Test
    fun `존재하지 않는 방에 join 요청 시 400과 에러 JSON을 반환한다`() {
        // given
        val roomId = RoomId("room-404")
        val memberId = MemberId("user-1")

        given(groupConversationService.joinRoom(roomId, memberId))
            .willThrow(IllegalArgumentException("Room not found: room-404"))

        val request = GroupJoinRoomRequest(
            memberId = "user-1"
        )
        val json = objectMapper.writeValueAsString(request)

        // when & then
        mockMvc.post("/api/group/rooms/${roomId.value}/join") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("INVALID_INPUT") }
            jsonPath("$.error.message") { value("Room not found: room-404") }
        }
    }

    @Test
    fun `방에 속하지 않은 멤버가 메시지를 보내면 400과 에러 JSON을 반환한다`() {
        // given
        val roomId = RoomId("room-1")
        val intruder = MemberId("intruder")

        given(
            groupConversationService.handleMessage(
                roomId = roomId,
                memberId = intruder,
                input = "hi"
            )
        ).willThrow(
            IllegalArgumentException("Member intruder is not in room room-1")
        )

        val request = GroupMessageRequest(
            memberId = "intruder",
            input = "hi"
        )
        val json = objectMapper.writeValueAsString(request)

        // when & then
        mockMvc.post("/api/group/rooms/${roomId.value}/messages") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("INVALID_INPUT") }
            jsonPath("$.error.message") { value("Member intruder is not in room room-1") }
        }
    }
}
