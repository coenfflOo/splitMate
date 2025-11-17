package com.splitmate.screens

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

@Composable
fun HomeScreen(
    goSolo: () -> Unit,
    goMenu: () -> Unit,
    goGroup: () -> Unit
) {
    Div({
        style {
            padding(32.px)
            fontFamily("Inter", "sans-serif")
        }
    }) {
        H1 { Text("SplitMate") }
        P { Text("영수증 N분의1 · 메뉴별 계산 · 그룹 실시간 계산을 지원합니다") }

        Div({
            style {
                marginTop(24.px)
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(16.px)
                width(240.px)
            }
        }) {
            Button(attrs = {
                onClick { goSolo() }
            }) { Text("SOLO 계산") }

            Button(attrs = {
                onClick { goMenu() }
            }) { Text("메뉴별 계산") }

            Button(attrs = {
                onClick { goGroup() }
            }) { Text("GROUP 모드") }
        }
    }
}
