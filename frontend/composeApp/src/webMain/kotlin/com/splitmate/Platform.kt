package com.splitmate

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform