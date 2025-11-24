package com.splitmate.screens

import androidx.compose.runtime.Composable
import com.splitmate.styles.AppStyles
import org.jetbrains.compose.web.dom.*

@Composable
fun HomeScreen(
    goSolo: () -> Unit,
    goMenu: () -> Unit,
    goGroup: () -> Unit
) {
    H1 { Text("SplitMate") }
    P {
        Text("영수증 N분의 1 / 메뉴별 계산 / GROUP 모드를 웹에서 편하게 사용해보세요.")
    }

    Div({ classes(AppStyles.buttonRow) }) {
        Button(attrs = { onClick { goSolo() } }) {
            Text("SOLO")
        }

        Button(attrs = { onClick { goMenu() } }) {
            Text("MENU")
        }

        Button(attrs = { onClick { goGroup() } }) {
            Text("GROUP")
        }
    }
}