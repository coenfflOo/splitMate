package com.splitmate.api

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

suspend fun callCreateRoom(req: GroupCreateRoomRequestDto): GroupRoomResponseDto {
    return httpClient.post("$BASE_URL/api/group/rooms") {
        contentType(ContentType.Application.Json)
        setBody(req)
    }.body()
}

suspend fun callJoinRoom(roomId: String, req: GroupJoinRoomRequestDto): GroupRoomResponseDto {
    return httpClient.post("$BASE_URL/api/group/rooms/$roomId/join") {
        contentType(ContentType.Application.Json)
        setBody(req)
    }.body()
}

suspend fun callGetRoom(roomId: String): GroupRoomResponseDto {
    return httpClient.get("$BASE_URL/api/group/rooms/$roomId") {
        contentType(ContentType.Application.Json)
    }.body()
}

suspend fun callSendGroupMessage(
    roomId: String,
    req: GroupMessageRequestDto
): GroupRoomResponseDto {
    return httpClient.post("$BASE_URL/api/group/rooms/$roomId/messages") {
        contentType(ContentType.Application.Json)
        setBody(req)
    }.body()
}
