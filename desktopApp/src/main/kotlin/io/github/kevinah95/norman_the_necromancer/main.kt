package io.github.kevinah95.norman_the_necromancer

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "NormanTheNecromancer",
    ) {
        App()
    }
}