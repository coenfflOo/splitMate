package adapter.websocket

import adapter.http.dto.GroupRoomResponse
import application.group.RoomState

fun RoomState.toResponse(): GroupRoomResponse =
    GroupRoomResponse(
        roomId = id.value,
        members = members.map { it.value }.sorted(),
        message = lastOutput.message,
        nextStep = lastOutput.nextStep.name
    )
