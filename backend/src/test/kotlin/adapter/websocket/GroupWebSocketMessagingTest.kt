package adapter.websocket

import adapter.http.dto.GroupMessageRequest
import adapter.http.dto.GroupRoomResponse
import application.group.GroupConversationService
import application.group.MemberId
import application.group.RoomId
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import config.AppConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.*
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import java.lang.reflect.Type
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [AppConfig::class]
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GroupWebSocketMessagingTest {

    @LocalServerPort
    private var port: Int = 0

    @MockitoBean
    lateinit var groupConversationService: GroupConversationService

    private lateinit var stompClient: WebSocketStompClient

    @BeforeEach
    fun setUp() {
        val mapper = jacksonObjectMapper() // Kotlin 모듈 등록된 ObjectMapper

        stompClient = WebSocketStompClient(StandardWebSocketClient()).apply {
            messageConverter = MappingJackson2MessageConverter().apply {
                objectMapper = mapper
            }
        }
    }
    @Test
    fun `클라이언트가 room topic을 구독하고 메시지를 보내면 브로드캐스트를 수신한다`() {
        // given
        val url = "ws://localhost:$port/ws"
        val roomId = RoomId("room-1")
        val memberId = MemberId("member-1")

        // 1) WebSocket이 브로드캐스트할 때 사용할 가짜 RoomState 준비
        val fakeOutput = domain.conversation.ConversationOutput(
            message = "ok!",
            nextStep = domain.conversation.ConversationStep.ASK_TOTAL_AMOUNT,
            context = application.conversation.ConversationContext() // 실제 타입에 맞춰서 or mock()
        )
        val fakeState = application.group.RoomState(
            id = roomId,
            members = setOf(memberId),
            lastOutput = fakeOutput
        )

        // 2) WebSocket 핸들러가 service.handleMessage() 호출했을 때 fakeState 반환하도록 stub
        given(
            groupConversationService.handleMessage(
                roomId,
                memberId,
                "10000"
            )
        ).willReturn(fakeState)

        val blockingQueue = ArrayBlockingQueue<GroupRoomResponse>(1)

        // 3) STOMP 연결
        val future = stompClient.connectAsync(
            url,
            object : StompSessionHandlerAdapter() {
                override fun handleException(
                    session: StompSession,
                    command: StompCommand?,
                    headers: StompHeaders,
                    payload: ByteArray,
                    exception: Throwable
                ) {
                    println("STOMP handleException: $exception")
                }

                override fun handleTransportError(session: StompSession, exception: Throwable) {
                    println("STOMP transport error: $exception")
                }
            }
        )

        val session = future.get(5, TimeUnit.SECONDS)
        assertTrue(session.isConnected)

        // 4) /topic/group/{roomId} 구독
        session.subscribe("/topic/group/${roomId.value}", object : StompFrameHandler {
            override fun getPayloadType(headers: StompHeaders): Type =
                GroupRoomResponse::class.java

            override fun handleFrame(headers: StompHeaders, payload: Any?) {
                blockingQueue.offer(payload as GroupRoomResponse)
            }
        })

        // 5) /app/group/{roomId}/messages 로 메시지 전송
        val request = GroupMessageRequest(
            memberId = memberId.value,
            input = "10000"
        )
        session.send("/app/group/${roomId.value}/messages", request)

        // when
        val received = blockingQueue.poll(2, TimeUnit.SECONDS)

        // then
        requireNotNull(received) { "브로드캐스트 메시지를 수신하지 못했습니다." }

        assertEquals(roomId.value, received.roomId)
        assertTrue(received.members.contains(memberId.value))
        assertEquals("ok!", received.message)

        session.disconnect()
    }
}
