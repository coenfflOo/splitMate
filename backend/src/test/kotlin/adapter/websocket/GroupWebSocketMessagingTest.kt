package adapter.websocket

import adapter.http.dto.GroupMessageRequest
import adapter.http.dto.GroupRoomResponse
import application.conversation.ConversationOutput
import application.group.GroupConversationService
import application.group.MemberId
import application.group.RoomId
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import config.AppConfig
import application.conversation.ConversationContext
import application.conversation.ConversationStep
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
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
        val fakeOutput = ConversationOutput(
            message = "ok!",
            nextStep = ConversationStep.ASK_TOTAL_AMOUNT,
            context = ConversationContext() // 실제 타입에 맞춰서 or mock()
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

    @Test
    fun `없는 roomId로 메시지 전송하면 ROOM_NOT_FOUND 에러가 브로드캐스트된다`() {
        // given
        val url = "ws://localhost:$port/ws"
        val roomId = RoomId("room-404")
        val memberId = MemberId("member-1")

        // 서비스가 RoomNotFoundException 던지도록 스텁
        given(
            groupConversationService.handleMessage(
                roomId,
                memberId,
                "hi"
            )
        ).willThrow(application.group.RoomNotFoundException(roomId))

        val blockingQueue = ArrayBlockingQueue<adapter.http.dto.ErrorResponse>(1)

        // STOMP 연결
        val session = stompClient.connectAsync(
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
        ).get(5, TimeUnit.SECONDS)

        assertTrue(session.isConnected)

        // 에러 토픽 구독: /topic/group/{roomId}.errors
        session.subscribe("/topic/group/${roomId.value}.errors", object : StompFrameHandler {
            override fun getPayloadType(headers: StompHeaders): Type =
                adapter.http.dto.ErrorResponse::class.java

            override fun handleFrame(headers: StompHeaders, payload: Any?) {
                blockingQueue.offer(payload as adapter.http.dto.ErrorResponse)
            }
        })

        // when: /app/group/{roomId}/messages 로 전송
        val request = GroupMessageRequest(
            memberId = memberId.value,
            input = "hi"
        )
        session.send("/app/group/${roomId.value}/messages", request)

        val error = blockingQueue.poll(2, TimeUnit.SECONDS)

        // then
        requireNotNull(error) { "에러 브로드캐스트를 수신하지 못했습니다." }

        assertEquals("ROOM_NOT_FOUND", error.error.code)
        // 메시지는 REST와 최대한 비슷하게
        println("WebSocket error message = ${error.error.message}")

        session.disconnect()
    }

    @Test
    fun `멤버가 방에 속해있지 않으면 INVALID_INPUT 에러가 브로드캐스트된다`() {
        // given
        val url = "ws://localhost:$port/ws"
        val roomId = RoomId("room-1")
        val memberId = MemberId("intruder")

        given(
            groupConversationService.handleMessage(
                roomId,
                memberId,
                "hi"
            )
        ).willThrow(IllegalArgumentException("Member ${memberId.value} is not in room ${roomId.value}"))

        val blockingQueue = ArrayBlockingQueue<adapter.http.dto.ErrorResponse>(1)

        val session = stompClient.connectAsync(
            url,
            object : StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS)

        assertTrue(session.isConnected)

        session.subscribe("/topic/group/${roomId.value}.errors", object : StompFrameHandler {
            override fun getPayloadType(headers: StompHeaders): Type =
                adapter.http.dto.ErrorResponse::class.java

            override fun handleFrame(headers: StompHeaders, payload: Any?) {
                blockingQueue.offer(payload as adapter.http.dto.ErrorResponse)
            }
        })

        val request = GroupMessageRequest(
            memberId = memberId.value,
            input = "hi"
        )
        session.send("/app/group/${roomId.value}/messages", request)

        // when
        val error = blockingQueue.poll(2, TimeUnit.SECONDS)

        // then
        requireNotNull(error) { "에러 브로드캐스트를 수신하지 못했습니다." }

        assertEquals("INVALID_INPUT", error.error.code)
        assertTrue(error.error.message.contains("Member intruder"))

        session.disconnect()
    }

    @Test
    fun `대화 context가 없으면 CONTEXT_MISSING 에러가 브로드캐스트된다`() {
        // given
        val url = "ws://localhost:$port/ws"
        val roomId = RoomId("room-1")
        val memberId = MemberId("member-1")

        given(
            groupConversationService.handleMessage(
                roomId,
                memberId,
                "hi"
            )
        ).willThrow(IllegalStateException("Context missing for room ${roomId.value}"))

        val blockingQueue = ArrayBlockingQueue<adapter.http.dto.ErrorResponse>(1)

        val session = stompClient.connectAsync(
            url,
            object : StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS)

        assertTrue(session.isConnected)

        session.subscribe("/topic/group/${roomId.value}.errors", object : StompFrameHandler {
            override fun getPayloadType(headers: StompHeaders): Type =
                adapter.http.dto.ErrorResponse::class.java

            override fun handleFrame(headers: StompHeaders, payload: Any?) {
                blockingQueue.offer(payload as adapter.http.dto.ErrorResponse)
            }
        })

        val request = GroupMessageRequest(
            memberId = memberId.value,
            input = "hi"
        )
        session.send("/app/group/${roomId.value}/messages", request)

        // when
        val error = blockingQueue.poll(2, TimeUnit.SECONDS)

        // then
        requireNotNull(error) { "에러 브로드캐스트를 수신하지 못했습니다." }

        assertEquals("CONTEXT_MISSING", error.error.code)
        assertTrue(error.error.message.contains("Context missing"))

        session.disconnect()
    }

}
