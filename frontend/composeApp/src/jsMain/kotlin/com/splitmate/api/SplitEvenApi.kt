package com.splitmate.api

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

suspend fun callSplitEven(request: SplitEvenRequestDto): SplitEvenResponseDto {
    val resp = httpClient.post("$BASE_URL/api/split/even") {
        contentType(ContentType.Application.Json)
        setBody(request)
    }

    if (!resp.status.isSuccess()) {
        throw IllegalStateException("HTTP ${resp.status}: ${resp.bodyAsText()}")
    }

    return resp.body()
}
