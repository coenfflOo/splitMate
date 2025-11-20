package com.splitmate

import androidx.compose.runtime.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import com.splitmate.screens.SoloSplitScreen

// 간단한 화면 enum
enum class Screen {
    Home,
    Solo,
    Menu,
    Group
}

@Composable
fun App() {
    Style(AppStyles)

    var currentScreen by remember { mutableStateOf(Screen.Home) }

    Div({ classes(AppStyles.page) }) {
        Div({ classes(AppStyles.card) }) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    goSolo = { currentScreen = Screen.Solo },
                    goMenu = { currentScreen = Screen.Menu },
                    goGroup = { currentScreen = Screen.Group }
                )

                Screen.Solo -> SoloSplitScreen(
                    goHome = { currentScreen = Screen.Home }
                )

                Screen.Menu -> {
                    // TODO: 메뉴별 계산 화면
                    P { Text("MENU 화면은 추후 구현 예정입니다.") }
                    Button(attrs = { onClick { currentScreen = Screen.Home } }) {
                        Text("← 홈으로")
                    }
                }

                Screen.Group -> {
                    // TODO: GROUP 모드 화면
                    P { Text("GROUP 화면은 추후 구현 예정입니다.") }
                    Button(attrs = { onClick { currentScreen = Screen.Home } }) {
                        Text("← 홈으로")
                    }
                }
            }
        }
    }
}

// 기존 홈 내용을 분리
@Composable
private fun HomeScreen(
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

    val formColumn by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Stretch)
        property("gap", "12px")
        marginTop(16.px)
        textAlign("left")
    }

    val textField by style {
        padding(8.px, 12.px)
        borderRadius(8.px)
        border {
            style(LineStyle.Solid)
            width(1.px)
            color(rgb(70, 80, 120))
        }
        backgroundColor(rgb(15, 18, 30))
        color(Color.white)
        fontSize(14.px)
    }

    val errorText by style {
        color(Color.red)
        fontSize(13.px)
    }

    val backButtonRow by style {
        display(DisplayStyle.Flex)
        justifyContent(JustifyContent.FlexStart)
        marginBottom(16.px)
    }

    val primaryButton by style {
        padding(10.px, 16.px)
        borderRadius(999.px)
        border {
            style(LineStyle.None)
        }
        backgroundColor(rgb(88, 101, 242))
        color(Color.white)
        fontSize(14.px)
        fontWeight("600")
        cursor("pointer")
        property("transition", "background-color 0.15s ease, transform 0.08s ease")

        hover {
            backgroundColor(rgb(100, 115, 255))
            property("transform", "translateY(-1px)")
        }

        active {
            property("transform", "translateY(0)")
            backgroundColor(rgb(70, 85, 220))
        }
    }
}
