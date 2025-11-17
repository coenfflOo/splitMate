package com.splitmate

import androidx.compose.runtime.*
import org.jetbrains.compose.web.renderComposable

fun main() {
    renderComposable(rootElementId = "root") {
        App()
    }
}
