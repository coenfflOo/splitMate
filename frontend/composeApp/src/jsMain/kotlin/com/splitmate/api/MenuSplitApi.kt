package com.splitmate.api

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

suspend fun callMenuSplit(request: MenuSplitRequestDto): MenuSplitResponseDto {
    return httpClient.post("$BASE_URL/api/split/by-menu") {
        contentType(ContentType.Application.Json)
        setBody(request)
    }.body()
}