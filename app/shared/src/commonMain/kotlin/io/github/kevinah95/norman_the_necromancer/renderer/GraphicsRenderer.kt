package io.github.kevinah95.norman_the_necromancer.renderer

import io.github.kevinah95.norman_the_necromancer.assets.GameAtlas
import io.github.kevinah95.norman_the_necromancer.assets.GameSprites
import io.github.kevinah95.norman_the_necromancer.core.GameLayout
import io.github.kevinah95.norman_the_necromancer.core.Rect2D
import korlibs.image.bitmap.sliceWithSize
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.render.BatchBuilder2D
import korlibs.korge.render.RenderContext

/**
 * Low-level 2D graphics and text rendering engine.
 *
 * Encapsulates texture slice extraction, 9-slice window borders,
 * bitmap font typography, and coordinate conversion between world-space and screen-space.
 */
class GraphicsRenderer(
    var sceneOriginY: Double = GameLayout.SCENE_ORIGIN_Y
) {
    /**
     * Draws a single sprite quad directly at screen coordinates (X, Y).
     */
    fun drawSprite(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        name: String,
        x: Double,
        y: Double,
        w: Double? = null,
        h: Double? = null
    ) {
        val slice = GameAtlas.getSprite(name) ?: return
        val tex = ctx.getTex(slice)
        val width = (w ?: slice.width.toDouble()).toFloat()
        val height = (h ?: slice.height.toDouble()).toFloat()
        batch.drawQuad(tex, x.toFloat(), y.toFloat(), width, height, filtering = false)
    }

    /**
     * Draws a sprite in world scene coordinates:
     * Ground level is at y = 0.0 (which translates to screen Y = sceneOriginY - sprite.height).
     * Positive world Y moves upwards towards the ceiling.
     */
    fun drawSceneSprite(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        name: String,
        x: Double,
        y: Double,
        shakeX: Double = 0.0,
        shakeY: Double = 0.0
    ) {
        val slice = GameAtlas.getSprite(name) ?: return
        val screenX = x + shakeX
        val screenY = sceneOriginY - y - slice.height + shakeY
        drawSprite(batch, ctx, name, screenX, screenY)
    }

    /**
     * Renders a resizable 9-slice framed panel using 3px corner slices.
     */
    fun drawNineSlice(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        name: String,
        x: Double,
        y: Double,
        w: Double,
        h: Double
    ) {
        val rect = GameAtlas.spriteRects[name] ?: return
        val bmp = GameAtlas.bitmap
        val c = 3
        if (w <= c || h <= c) return

        val sw = rect.w
        val sh = rect.h
        val sw1 = (sw - 2 * c).coerceAtLeast(1)
        val sh1 = (sh - 2 * c).coerceAtLeast(1)

        val dx1 = x
        val dx2 = x + c
        val dx3 = x + w - c
        val dy1 = y
        val dy2 = y + c
        val dy3 = y + h - c
        val dw1 = (dx3 - dx2).coerceAtLeast(0.0)
        val dh1 = (dy3 - dy2).coerceAtLeast(0.0)

        // Draw corners
        drawSliceRect(batch, ctx, bmp, rect.x, rect.y, c, c, dx1, dy1, c.toDouble(), c.toDouble())
        drawSliceRect(batch, ctx, bmp, rect.x + sw - c, rect.y, c, c, dx3, dy1, c.toDouble(), c.toDouble())
        drawSliceRect(batch, ctx, bmp, rect.x, rect.y + sh - c, c, c, dx1, dy3, c.toDouble(), c.toDouble())
        drawSliceRect(batch, ctx, bmp, rect.x + sw - c, rect.y + sh - c, c, c, dx3, dy3, c.toDouble(), c.toDouble())

        // Draw edges
        drawSliceRect(batch, ctx, bmp, rect.x + c, rect.y, sw1, c, dx2, dy1, dw1, c.toDouble())
        drawSliceRect(batch, ctx, bmp, rect.x + c, rect.y + sh - c, sw1, c, dx2, dy3, dw1, c.toDouble())
        drawSliceRect(batch, ctx, bmp, rect.x, rect.y + c, c, sh1, dx1, dy2, c.toDouble(), dh1)
        drawSliceRect(batch, ctx, bmp, rect.x + sw - c, rect.y + c, c, sh1, dx3, dy2, c.toDouble(), dh1)

        // Draw center
        drawSliceRect(batch, ctx, bmp, rect.x + c, rect.y + c, sw1, sh1, dx2, dy2, dw1, dh1)
    }

    private fun drawSliceRect(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        bmp: korlibs.image.bitmap.Bitmap,
        sx: Int, sy: Int, sw: Int, sh: Int,
        dx: Double, dy: Double, dw: Double, dh: Double
    ) {
        if (dw <= 0 || dh <= 0) return
        val slice = bmp.sliceWithSize(sx, sy, sw, sh)
        val tex = ctx.getTex(slice)
        batch.drawQuad(tex, dx.toFloat(), dy.toFloat(), dw.toFloat(), dh.toFloat(), filtering = false)
    }

    /**
     * Renders bitmap font text with support for newlines ('\n') and variable glyph widths.
     */
    fun write(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        text: String,
        startX: Double,
        startY: Double,
        colorMul: RGBA = Colors.WHITE
    ) {
        var cursorX = startX
        var cursorY = startY

        for (char in text) {
            if (char == '\n') {
                cursorX = startX
                cursorY += GameAtlas.LINE_HEIGHT
            } else {
                val slice = GameAtlas.glyphSlices[char]
                if (slice != null) {
                    val tex = ctx.getTex(slice)
                    batch.drawQuad(
                        tex,
                        cursorX.toFloat(),
                        cursorY.toFloat(),
                        GameAtlas.GLYPH_WIDTH.toFloat(),
                        GameAtlas.GLYPH_HEIGHT.toFloat(),
                        filtering = false,
                        colorMul = colorMul
                    )
                }
                cursorX += GameAtlas.getGlyphWidth(char)
            }
        }
    }

    /**
     * Calculates the pixel width of [text], taking multiline strings into account.
     */
    fun getTextWidth(text: String): Double {
        var maxW = 0.0
        var curW = 0.0
        for (char in text) {
            if (char == '\n') {
                if (curW > maxW) maxW = curW
                curW = 0.0
            } else {
                curW += GameAtlas.getGlyphWidth(char)
            }
        }
        return if (curW > maxW) curW else maxW
    }

    /**
     * Renders [text] centered horizontally within [containerWidth] starting at [containerX].
     */
    fun writeCenteredHorizontally(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        text: String,
        y: Double,
        containerX: Double = 0.0,
        containerWidth: Double = GameLayout.VIRTUAL_WIDTH,
        colorMul: RGBA = Colors.WHITE
    ) {
        val textW = getTextWidth(text)
        val textX = containerX + (containerWidth - textW) / 2.0
        write(batch, ctx, text, textX, y, colorMul)
    }

    /**
     * Renders a multiline [text] block centered both horizontally and around [centerY].
     */
    fun writeCentered(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        text: String,
        centerY: Double,
        lineSpacing: Double = 11.0
    ) {
        val lines = text.split('\n')
        val totalHeight = (lines.size - 1) * lineSpacing + GameAtlas.GLYPH_HEIGHT
        var startY = centerY - totalHeight / 2.0
        for (line in lines) {
            writeCenteredHorizontally(batch, ctx, line, startY)
            startY += lineSpacing
        }
    }

    /**
     * Renders an interactive button with a 9-slice frame and centered label.
     */
    fun drawButton(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        bounds: Rect2D,
        text: String,
        frameSprite: String = GameSprites.PINK_FRAME,
        textOffsetY: Double = 6.0
    ) {
        drawNineSlice(batch, ctx, frameSprite, bounds.x, bounds.y, bounds.w, bounds.h)
        val textW = getTextWidth(text)
        val textX = bounds.x + (bounds.w - textW) / 2.0
        write(batch, ctx, text, textX, bounds.y + textOffsetY)
    }

    /**
     * Renders an orb meter (e.g. health or spell pips) centered around [centerX].
     */
    fun drawOrbs(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        centerX: Double,
        y: Double,
        value: Int,
        maxValue: Int,
        sprite: String,
        emptySprite: String,
        shakeX: Double = 0.0,
        shakeY: Double = 0.0
    ) {
        val x0 = centerX - (maxValue * 4.0) / 2.0
        for (i in 0 until maxValue) {
            val sName = if (i < value) sprite else emptySprite
            drawSceneSprite(batch, ctx, sName, x0 + i * 4.0, y, shakeX, shakeY)
        }
    }
}
