package com.splitmate.api

import com.splitmate.api.client.BASE_URL
import com.splitmate.api.client.httpClient
import com.splitmate.api.dto.GroupCreateRoomRequestDto
import com.splitmate.api.dto.GroupJoinRoomRequestDto
import com.splitmate.api.dto.GroupRoomResponseDto
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

suspend fun callCreateRoom(req: GroupCreateRoomRequestDto): GroupRoomResponseDto {
    return httpClient.post("${BASE_URL}/api/group/rooms") {
        contentType(ContentType.Application.Json)
        setBody(req)
    }.body()
}

suspend fun callJoinRoom(roomId: String, req: GroupJoinRoomRequestDto): GroupRoomResponseDto {
    return httpClient.post("${BASE_URL}/api/group/rooms/$roomId/join") {
        contentType(ContentType.Application.Json)
        setBody(req)
    }.body()
}