package adapter.http

import adapter.http.dto.GroupJoinRoomRequest
import adapter.http.dto.GroupMessageRequest
import application.group.GroupConversationService
import application.group.MemberId
import application.group.RoomId
import application.group.RoomNotFoundException
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
import org.springframework.test.web.servlet.get

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
        val roomId = RoomId("room-404")
        val memberId = MemberId("user-1")

        given(groupConversationService.joinRoom(roomId, memberId))
            .willThrow(IllegalArgumentException("Room not found: room-404"))

        val request = GroupJoinRoomRequest(
            memberId = "user-1"
        )
        val json = objectMapper.writeValueAsString(request)

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

        mockMvc.post("/api/group/rooms/${roomId.value}/messages") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("INVALID_INPUT") }
            jsonPath("$.error.message") { value("Member intruder is not in room room-1") }
        }
    }

    @Test
    fun `GET room - room not found returns 404`() {
        mockMvc.get("/api/group/rooms/no-such-room")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("ROOM_NOT_FOUND") }
            }
    }

    @Test
    fun `POST joinRoom - room not found returns 404`() {
        val roomId = RoomId("no-such-room")
        val memberId = MemberId("member-1")

        given(groupConversationService.joinRoom(roomId, memberId))
            .willThrow(RoomNotFoundException(roomId))

        mockMvc.post("/api/group/rooms/${roomId.value}/join") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                { "memberId": "${memberId.value}" }
            """.trimIndent()
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error.code") { value("ROOM_NOT_FOUND") }
        }
    }

    @Test
    fun `POST messages - room not found returns 404`() {
        val roomId = RoomId("no-such-room")
        val memberId = MemberId("member-1")

        given(groupConversationService.handleMessage(roomId, memberId, "hi"))
            .willThrow(RoomNotFoundException(roomId))

        mockMvc.post("/api/group/rooms/${roomId.value}/messages") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                { "memberId": "${memberId.value}", "input": "hi" }
            """.trimIndent()
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error.code") { value("ROOM_NOT_FOUND") }
        }
    }

    @Test
    fun `POST messages - member not in room returns 400`() {
        val roomId = RoomId("room-1")
        val memberId = MemberId("stranger")

        given(groupConversationService.handleMessage(roomId, memberId, "hi"))
            .willThrow(IllegalArgumentException("Member not in room"))

        mockMvc.post("/api/group/rooms/${roomId.value}/messages") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                { "memberId": "${memberId.value}", "input": "hi" }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("INVALID_INPUT") }
        }
    }

    @Test
    fun `POST messages - context missing returns 409`() {
        val roomId = RoomId("r2")
        val memberId = MemberId("owner")

        given(
            groupConversationService.handleMessage(roomId, memberId, "hello")
        ).willThrow(
            IllegalStateException("Context missing")
        )

        val body = """{ "memberId": "owner", "input": "hello" }"""

        mockMvc.post("/api/group/rooms/${roomId.value}/messages") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isConflict() }
            jsonPath("$.error.code") { value("CONTEXT_MISSING") }
            jsonPath("$.error.message") { value("Context missing") }
        }
    }
}
