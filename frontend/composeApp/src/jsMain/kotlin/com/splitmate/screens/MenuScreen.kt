package com.splitmate.screens

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.*

@Composable
fun MenuScreen(goHome: () -> Unit) {
    Div {
        H2 { Text("메뉴별 계산") }
        Button(attrs = {
            onClick { goHome() }
        }) { Text("← 홈으로") }
    }
}
