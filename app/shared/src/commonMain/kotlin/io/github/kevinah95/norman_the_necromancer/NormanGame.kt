package io.github.kevinah95.norman_the_necromancer

import io.github.kevinah95.norman_the_necromancer.core.GameLayout
import io.github.kevinah95.norman_the_necromancer.scene.NormanGameScene
import korlibs.image.color.Colors
import korlibs.korge.Korge
import korlibs.korge.scene.sceneContainer
import korlibs.math.geom.ScaleMode
import korlibs.math.geom.Size
import korlibs.render.GameWindow

/**
 * Common entry point for Norman The Necromancer.
 * Sets up KorGE with virtual resolution from [GameLayout], pixel-perfect aspect ratio scaling,
 * and launches the main game scene.
 */
suspend fun launchNormanGame() {
    Korge(
        virtualSize = Size(GameLayout.VIRTUAL_WIDTH_INT, GameLayout.VIRTUAL_HEIGHT_INT),
        windowSize = Size(GameLayout.VIRTUAL_WIDTH_INT * 2, GameLayout.VIRTUAL_HEIGHT_INT * 2),
        title = "Norman The Necromancer",
        backgroundColor = Colors["#181622"],
        quality = GameWindow.Quality.QUALITY,
        scaleMode = ScaleMode.SHOW_ALL
    ) {
        val sceneContainer = sceneContainer()
        sceneContainer.changeTo { NormanGameScene() }
    }
}
