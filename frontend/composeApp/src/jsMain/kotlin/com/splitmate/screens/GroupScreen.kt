package com.splitmate.screens

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.*

@Composable
fun GroupScreen(goHome: () -> Unit) {
    Div {
        H2 { Text("GROUP 모드") }
        Button(attrs = {
            onClick { goHome() }
        }) { Text("← 홈으로") }
    }
}
