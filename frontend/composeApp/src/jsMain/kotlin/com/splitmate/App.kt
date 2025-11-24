package com.splitmate

import androidx.compose.runtime.*
import com.splitmate.screens.GroupScreen
import com.splitmate.screens.MenuSplitScreen
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import com.splitmate.screens.SoloSplitScreen
import com.splitmate.styles.AppStyles
import com.splitmate.screens.HomeScreen

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

                Screen.Menu -> MenuSplitScreen(
                    goHome = { currentScreen = Screen.Home }
                )

                Screen.Group -> GroupScreen(
                    goHome = { currentScreen = Screen.Home }
                )
            }
        }
    }
}