package com.splitmate

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

@Composable
fun App() {
    Style(AppStyles)

    Div({ classes(AppStyles.page) }) {
        Div({ classes(AppStyles.card) }) {
            H1 { Text("SplitMate") }
            P {
                Text("영수증 N분의 1 / 메뉴별 계산 / GROUP 모드를 웹에서 편하게 사용해보세요.")
            }

            Div({ classes(AppStyles.buttonRow) }) {
                Button(attrs = {
                    onClick { /* TODO: SOLO 화면으로 이동 */ }
                }) {
                    Text("SOLO")
                }

                Button(attrs = {
                    onClick { /* TODO: 메뉴별 화면으로 이동 */ }
                }) {
                    Text("MENU")
                }

                Button(attrs = {
                    onClick { /* TODO: GROUP 화면으로 이동 */ }
                }) {
                    Text("GROUP")
                }
            }
        }
    }
}

object AppStyles : StyleSheet() {

    val page by style {
        property("min-height", "100vh")
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        justifyContent(JustifyContent.Center)
        alignItems(AlignItems.Center)
        backgroundColor(rgb(10, 13, 24))
        color(Color.white)
        fontFamily("system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif")
    }

    val card by style {
        padding(32.px)
        borderRadius(16.px)
        backgroundColor(rgb(20, 24, 40))
        property("box-shadow", "0 18px 45px rgba(0,0,0,0.45)")
        maxWidth(480.px)
        textAlign("center")
    }

    val buttonRow by style {
        marginTop(24.px)
        display(DisplayStyle.Flex)
        justifyContent(JustifyContent.Center)
        property("gap", "12px")
    }
}
