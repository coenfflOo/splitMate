package com.splitmate.websocket

import com.splitmate.websocket.dto.WsErrorMessage
import com.splitmate.websocket.dto.WsGroupMessage
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket
import org.w3c.dom.events.Event

class GroupStompClient(
    private val wsUrl: String = "ws://localhost:8080/ws"
) {

    private var socket: WebSocket? = null
    private var currentRoomId: String? = null


    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onGroupMessage: ((WsGroupMessage) -> Unit)? = null
    var onErrorMessage: ((WsErrorMessage) -> Unit)? = null
    var onConnectionError: ((String) -> Unit)? = null

    fun connect(roomId: String) {
        if (socket != null) {
            disconnect()
        }

        currentRoomId = roomId

        val ws = WebSocket(wsUrl)
        socket = ws

        ws.onopen = {
            sendFrame(
                command = "CONNECT",
                headers = mapOf(
                    "accept-version" to "1.2",
                    "host" to "localhost"
                )
            )

            sendFrame(
                command = "SUBSCRIBE",
                headers = mapOf(
                    "id" to "sub-room",
                    "destination" to "/topic/group/$roomId"
                )
            )

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

        ws.onclose = { _: Event ->
            socket = null
            currentRoomId = null
            onDisconnected?.invoke()
        }

        ws.onerror = { _: Event ->
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

        val escapedMember = memberId.replace("\"", "\\\"")
        val escapedInput = input.replace("\"", "\\\"")
        val body = """{"memberId":"$escapedMember","input":"$escapedInput"}"""

        sendFrame(
            command = "SEND",
            headers = mapOf(
                "destination" to "/app/group/$roomId/messages",
                "content-type" to "application/json"
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
                if (destination.endsWith(".errors")) {
                    handleErrorBody(body)
                } else {
                    handleGroupBody(body)
                }
            }

            "ERROR" -> {
                onConnectionError?.invoke("STOMP ERROR 프레임 수신: $body")
            }

            else -> {
            }
        }
    }

    private fun handleErrorBody(body: String) {
        val dyn = JSON.parse<dynamic>(body)
        val code = dyn.code as? String ?: "UNKNOWN"
        val msg = dyn.message as? String ?: "알 수 없는 에러"

        onErrorMessage?.invoke(
            WsErrorMessage(code = code, message = msg)
        )
    }

    private fun handleGroupBody(body: String) {
        val dyn = JSON.parse<dynamic>(body)

        val roomId = dyn.roomId as? String ?: ""
        val message = dyn.message as? String ?: ""
        val nextStep = dyn.nextStep as? String
        val senderId = dyn.senderId as? String
        val messageType = dyn.messageType as? String ?: "SYSTEM"

        val membersDyn = dyn.members
        val members: List<String> =
            if (membersDyn != null) {
                val arr = membersDyn.unsafeCast<Array<dynamic>>()
                arr.mapNotNull { it as? String }
            } else emptyList()

        onGroupMessage?.invoke(
            WsGroupMessage(
                roomId = roomId,
                members = members,
                message = message,
                nextStep = nextStep,
                senderId = senderId,
                messageType = messageType
            )
        )
    }
}