package com.splitmate.screens

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.*

@Composable
fun SoloScreen(goHome: () -> Unit) {
    Div {
        H2 { Text("SOLO 계산 (N분의 1)") }
        Button(attrs = {
            onClick { goHome() }
        }) { Text("← 홈으로") }
    }
}
