package com.splitmate

import androidx.compose.runtime.*
import com.splitmate.screens.HomeScreen
import com.splitmate.screens.GroupScreen
import com.splitmate.screens.SoloScreen
import com.splitmate.screens.MenuScreen

@Composable
fun App() {
    var route by remember { mutableStateOf(Route.HOME) }

    when (route) {
        Route.HOME -> HomeScreen(
            goSolo = { route = Route.SOLO },
            goMenu = { route = Route.MENU },
            goGroup = { route = Route.GROUP }
        )
        Route.SOLO -> SoloScreen(goHome = { route = Route.HOME })
        Route.MENU -> MenuScreen(goHome = { route = Route.HOME })
        Route.GROUP -> GroupScreen(goHome = { route = Route.HOME })
    }
}
