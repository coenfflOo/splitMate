package adapter.websocket

import application.group.GroupConversationService
import config.AppConfig
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import java.util.concurrent.TimeUnit

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [AppConfig::class]
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WebSocketConfigTest {

    @LocalServerPort
    private var port: Int = 0

    @MockitoBean  // 또는 @MockBean
    lateinit var groupConversationService: GroupConversationService

    private lateinit var stompClient: WebSocketStompClient

    @BeforeEach
    fun setUp() {
        stompClient = WebSocketStompClient(StandardWebSocketClient()).apply {
            messageConverter = MappingJackson2MessageConverter()
        }
    }

    @Test
//    @Disabled("WebSocketConfig 구현 후 활성화 예정")
    fun `STOMP 클라이언트가 ws 엔드포인트로 연결할 수 있다`() {
        // given
        val url = "ws://localhost:$port/ws"

        // when
        val future = stompClient.connectAsync(
            url,
            object : StompSessionHandlerAdapter() {}
        )

        val session = future.get(5, TimeUnit.SECONDS)

        // then
        assertTrue(session.isConnected)

        session.disconnect()
    }
}
