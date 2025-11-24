package com.splitmate.api

import com.splitmate.api.client.BASE_URL
import com.splitmate.api.client.httpClient
import com.splitmate.api.dto.MenuSplitRequestDto
import com.splitmate.api.dto.MenuSplitResponseDto
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

suspend fun callMenuSplit(request: MenuSplitRequestDto): MenuSplitResponseDto {
    return httpClient.post("${BASE_URL}/api/split/by-menu") {
        contentType(ContentType.Application.Json)
        setBody(request)
    }.body()
}