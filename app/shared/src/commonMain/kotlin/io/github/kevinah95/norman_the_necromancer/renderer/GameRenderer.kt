package io.github.kevinah95.norman_the_necromancer.renderer

import io.github.kevinah95.norman_the_necromancer.core.*
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.render.BatchBuilder2D
import korlibs.korge.render.RenderContext

/**
 * Top-level rendering orchestrator for Norman The Necromancer.
 *
 * Implements the **Facade pattern**, delegating specialized drawing tasks to:
 * - [GraphicsRenderer]: Low-level 2D texture quads, 9-slice panels, and bitmap font typography.
 * - [WorldRenderer]: Game arena, animated particles, entities, status effects, and aiming reticle.
 * - [UiRenderer]: Heads-up display (HUD), inter-wave shop, dialogue screens, and pause overlays.
 */
class GameRenderer(
    initialSceneOriginY: Double = GameLayout.SCENE_ORIGIN_Y
) {
    val graphics: GraphicsRenderer = GraphicsRenderer(initialSceneOriginY)
    val worldRenderer: WorldRenderer = WorldRenderer(graphics)
    val uiRenderer: UiRenderer = UiRenderer(graphics)

    var sceneOriginY: Double
        get() = graphics.sceneOriginY
        set(value) { graphics.sceneOriginY = value }

    fun render(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        game: Game,
        isPaused: Boolean,
        touchAimActive: Boolean
    ) {
        val shakeX = if (TweenManager.screenShakeTimer > 0) randomInt(2).toDouble() else 0.0
        val shakeY = if (TweenManager.screenShakeTimer > 0) randomInt(2).toDouble() else 0.0

        // 1. World Layer (Background, Particles, Entities, Targeting Reticle)
        worldRenderer.render(batch, ctx, game, shakeX, shakeY, touchAimActive)

        // 2. UI & Overlays (HUD, Shop, Dialogue, Pause Screen, Watermark)
        uiRenderer.render(batch, ctx, game, isPaused)
    }

    // --- Delegation methods for backwards compatibility and unit testing ---

    fun getTextWidth(text: String): Double = graphics.getTextWidth(text)

    fun write(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        text: String,
        startX: Double,
        startY: Double,
        colorMul: RGBA = Colors.WHITE
    ) = graphics.write(batch, ctx, text, startX, startY, colorMul)

    fun writeCentered(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        text: String,
        centerY: Double,
        lineSpacing: Double = 11.0
    ) = graphics.writeCentered(batch, ctx, text, centerY, lineSpacing)

    fun writeCenteredHorizontally(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        text: String,
        y: Double,
        containerX: Double = 0.0,
        containerWidth: Double = GameLayout.VIRTUAL_WIDTH,
        colorMul: RGBA = Colors.WHITE
    ) = graphics.writeCenteredHorizontally(batch, ctx, text, y, containerX, containerWidth, colorMul)

    fun drawSprite(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        name: String,
        x: Double,
        y: Double,
        w: Double? = null,
        h: Double? = null
    ) = graphics.drawSprite(batch, ctx, name, x, y, w, h)

    fun drawSceneSprite(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        name: String,
        x: Double,
        y: Double,
        shakeX: Double = 0.0,
        shakeY: Double = 0.0
    ) = graphics.drawSceneSprite(batch, ctx, name, x, y, shakeX, shakeY)

    fun drawNineSlice(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        name: String,
        x: Double,
        y: Double,
        w: Double,
        h: Double
    ) = graphics.drawNineSlice(batch, ctx, name, x, y, w, h)

    fun drawButton(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        bounds: Rect2D,
        text: String,
        frameSprite: String = io.github.kevinah95.norman_the_necromancer.assets.GameSprites.PINK_FRAME,
        textOffsetY: Double = 6.0
    ) = graphics.drawButton(batch, ctx, bounds, text, frameSprite, textOffsetY)
}
