package com.splitmate.websocket

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.w3c.dom.CloseEvent
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket

private val json = Json {
    ignoreUnknownKeys = true
}

class GroupStompClient(
    private val wsUrl: String = "ws://localhost:8080/ws"
) {

    private val scope = MainScope()

    private var socket: WebSocket? = null
    private var currentRoomId: String? = null

    // 외부에서 주입받을 콜백들
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onGroupMessage: ((WsGroupMessage) -> Unit)? = null
    var onErrorMessage: ((WsErrorMessage) -> Unit)? = null
    var onConnectionError: ((String) -> Unit)? = null

    fun connect(roomId: String) {
        if (socket != null) {
            // 이미 열려 있으면 일단 닫고 새로 열기
            disconnect()
        }

        currentRoomId = roomId

        val ws = WebSocket(wsUrl)
        socket = ws

        ws.onopen = {
            // STOMP CONNECT
            sendFrame(
                command = "CONNECT",
                headers = mapOf(
                    "accept-version" to "1.2",
                    "host" to "localhost"
                )
            )

            // 방 메시지 구독
            sendFrame(
                command = "SUBSCRIBE",
                headers = mapOf(
                    "id" to "sub-room",
                    "destination" to "/topic/group/$roomId"
                )
            )

            // 에러 토픽 구독
            sendFrame(
                command = "SUBSCRIBE",
                headers = mapOf(
                    "id" to "sub-error",
                    "destination" to "/topic/group/$roomId.errors"
                )
            )

            onConnected?.invoke()
        }

        ws.onmessage = { event ->
            val msgEvent = event as MessageEvent
            val data = msgEvent.data
            if (data is String) {
                handleIncomingFrame(data)
            }
        }

        ws.onclose = { _: CloseEvent ->
            socket = null
            currentRoomId = null
            onDisconnected?.invoke()
        }

        ws.onerror = {
            onConnectionError?.invoke("WebSocket 오류가 발생했습니다.")
        }
    }

    fun disconnect() {
        socket?.close()
        socket = null
        currentRoomId = null
    }

    fun sendGroupInput(memberId: String, input: String) {
        val roomId = currentRoomId
        val ws = socket

        if (roomId == null || ws == null) {
            onConnectionError?.invoke("방에 연결되지 않았습니다.")
            return
        }

        val payload = GroupInputPayload(memberId = memberId, input = input)
        val body = json.encodeToString(GroupInputPayload.serializer(), payload)

        sendFrame(
            command = "SEND",
            headers = mapOf(
                "destination" to "/app/group/$roomId/messages"
            ),
            body = body
        )
    }

    private fun sendFrame(
        command: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null
    ) {
        val ws = socket ?: return

        val builder = StringBuilder()
        builder.append(command).append('\n')
        headers.forEach { (k, v) ->
            builder.append(k).append(':').append(v).append('\n')
        }
        builder.append('\n')
        if (body != null) {
            builder.append(body)
        }
        builder.append('\u0000')

        ws.send(builder.toString())
    }

    private fun handleIncomingFrame(text: String) {
        // 아주 단순한 STOMP 파서: 첫 줄 = COMMAND, 그 뒤 헤더, 빈 줄 이후 body
        val headerEndIndex = text.indexOf("\n\n")
        if (headerEndIndex == -1) return

        val headerPart = text.substring(0, headerEndIndex)
        val bodyPartRaw = text.substring(headerEndIndex + 2)
        val body = bodyPartRaw.trim('\u0000', '\r', '\n')

        val lines = headerPart.split('\n')
        if (lines.isEmpty()) return

        val command = lines[0].trim()
        val headers = lines
            .drop(1)
            .filter { it.contains(':') }
            .associate { line ->
                val idx = line.indexOf(':')
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim()
                key to value
            }

        when (command) {
            "MESSAGE" -> {
                val destination = headers["destination"].orEmpty()
                scope.launch {
                    if (destination.endsWith(".errors")) {
                        // 에러 토픽
                        runCatching {
                            json.decodeFromString(WsErrorMessage.serializer(), body)
                        }.onSuccess { err ->
                            onErrorMessage?.invoke(err)
                        }.onFailure {
                            onConnectionError?.invoke("에러 메시지 파싱 실패: $body")
                        }
                    } else {
                        // 일반 그룹 메시지
                        runCatching {
                            json.decodeFromString(WsGroupMessage.serializer(), body)
                        }.onSuccess { msg ->
                            onGroupMessage?.invoke(msg)
                        }.onFailure {
                            onConnectionError?.invoke("메시지 파싱 실패: $body")
                        }
                    }
                }
            }

            "ERROR" -> {
                scope.launch {
                    onConnectionError?.invoke("STOMP ERROR 프레임 수신: $body")
                }
            }

            else -> {
                // CONNECTED 등은 현재 특별한 처리 없이 무시
            }
        }
    }
}
