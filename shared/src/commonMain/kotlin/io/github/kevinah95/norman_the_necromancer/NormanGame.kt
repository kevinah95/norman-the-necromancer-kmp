package io.github.kevinah95.norman_the_necromancer

import io.github.kevinah95.norman_the_necromancer.scene.NormanGameScene
import korlibs.image.color.Colors
import korlibs.korge.Korge
import korlibs.korge.scene.sceneContainer
import korlibs.math.geom.ScaleMode
import korlibs.math.geom.Size

/**
 * Common entry point for Norman The Necromancer.
 * Sets up KorGE with a 400x200 virtual resolution, pixel-perfect aspect ratio scaling,
 * and launches the main game scene.
 */
suspend fun launchNormanGame() {
    Korge(
        virtualSize = Size(400, 200),
        windowSize = Size(800, 400),
        title = "Norman The Necromancer",
        backgroundColor = Colors["#000000"],
        scaleMode = ScaleMode.SHOW_ALL
    ) {
        val sceneContainer = sceneContainer()
        sceneContainer.changeTo { NormanGameScene() }
    }
}
