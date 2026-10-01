package io.github.kevinah95.norman_the_necromancer.renderer

import io.github.kevinah95.norman_the_necromancer.assets.GameAtlas
import io.github.kevinah95.norman_the_necromancer.behaviours.Frozen
import io.github.kevinah95.norman_the_necromancer.core.*
import io.github.kevinah95.norman_the_necromancer.fx.Fx
import io.github.kevinah95.norman_the_necromancer.shop.ShopManager
import korlibs.image.bitmap.sliceWithSize
import korlibs.korge.render.BatchBuilder2D
import korlibs.korge.render.RenderContext
import kotlin.math.roundToInt

class GameRenderer {
    var sceneOriginY: Double = 150.0

    fun render(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        game: Game,
        isPaused: Boolean,
        touchAimActive: Boolean
    ) {
        val shakeX = if (TweenManager.screenShakeTimer > 0) (randomInt(2)).toDouble() else 0.0
        val shakeY = if (TweenManager.screenShakeTimer > 0) (randomInt(2)).toDouble() else 0.0

        // 1. Draw World Scene (Background, Particles, Objects, Reticle)
        drawBackground(batch, ctx, game, shakeX, shakeY)
        drawParticles(batch, ctx, shakeX, shakeY)
        drawObjects(batch, ctx, game, shakeX, shakeY)

        if (game.state == GameState.PLAYING) {
            drawReticle(batch, ctx, game, shakeX, shakeY, touchAimActive)
        }

        // 2. Draw HUD & UI overlays
        drawHud(batch, ctx, game, isPaused)

        if (game.state == GameState.SHOPPING) {
            drawShop(batch, ctx, game)
        }

        drawDialogue(batch, ctx, game)

        if (isPaused) {
            drawPauseOverlay(batch, ctx)
        }
    }

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
     * Draw sprite in scene coordinates:
     * Ground is at y = 0 (which translates to screen Y = sceneOriginY - sprite.height).
     * Positive y moves upwards!
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

    fun write(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        text: String,
        startX: Double,
        startY: Double
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
                    batch.drawQuad(tex, cursorX.toFloat(), cursorY.toFloat(), GameAtlas.GLYPH_WIDTH.toFloat(), GameAtlas.GLYPH_HEIGHT.toFloat(), filtering = false)
                }
                cursorX += GameAtlas.getGlyphWidth(char)
            }
        }
    }

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
            val width = getTextWidth(line)
            val startX = (400.0 - width) / 2.0
            write(batch, ctx, line, startX, startY)
            startY += lineSpacing
        }
    }

    private fun drawBackground(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        game: Game,
        shakeX: Double,
        shakeY: Double
    ) {
        val count = (game.stage.width / 16.0).toInt()
        for (i in 0 until count) {
            val wallSprite = if (i % 5 != 0) "wall" else "door"
            drawSceneSprite(batch, ctx, wallSprite, i * 16.0, 0.0, shakeX, shakeY)
            drawSceneSprite(batch, ctx, "floor", i * 16.0, -8.0, shakeX, shakeY)
            drawSceneSprite(batch, ctx, "ceiling", i * 16.0, game.stage.ceiling, shakeX, shakeY)
        }
    }

    private fun drawParticles(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        shakeX: Double,
        shakeY: Double
    ) {
        for (emitter in Fx.activeEmitters) {
            for (p in emitter.particles) {
                if (emitter.variants.isNotEmpty() && p.variantIndex < emitter.variants.size) {
                    val variant = emitter.variants[p.variantIndex]
                    if (variant.isNotEmpty()) {
                        val progress = clamp(p.elapsed / p.duration, 0.0, 1.0)
                        val spriteIndex = (progress * variant.size).toInt().coerceIn(0, variant.size - 1)
                        val spriteName = variant[spriteIndex]
                        drawSceneSprite(batch, ctx, spriteName, p.x, p.y, shakeX, shakeY)
                    }
                }
            }
        }
    }

    private fun drawObjects(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        game: Game,
        shakeX: Double,
        shakeY: Double
    ) {
        for (obj in game.objects) {
            drawSceneSprite(batch, ctx, obj.spriteName, obj.x, obj.y + obj.hop, shakeX, shakeY)

            // Frozen overlay
            if (obj.getBehaviour<Frozen>() != null) {
                val slice = GameAtlas.getSprite(obj.spriteName)
                val sw = slice?.width ?: obj.spriteWidth
                val sh = slice?.height ?: obj.spriteHeight
                val screenX = obj.x + shakeX
                val screenY = sceneOriginY - obj.y - sh + shakeY
                drawNineSlice(batch, ctx, "ice", screenX, screenY, sw.toDouble(), sh.toDouble())
            }

            // Health orbs
            if (obj.maxHp > 1 && obj !== game.player) {
                if (obj.maxHp < 10) {
                    val cx = obj.center().x
                    drawOrbs(batch, ctx, cx, -6.0, obj.hp, obj.maxHp, "health_orb", "health_orb_empty", shakeX, shakeY)
                } else {
                    drawSceneSprite(batch, ctx, "health_orb", obj.x, -6.0, shakeX, shakeY)
                    val screenX = obj.x + 6.0 + shakeX
                    val screenY = sceneOriginY - 0.0 + shakeY
                    write(batch, ctx, "${obj.hp}/${obj.maxHp}", screenX, screenY)
                }
            }

            // Status behaviour sprites
            var statusX = obj.x
            for (b in obj.behaviours) {
                val sName = b.spriteName
                if (sName != null) {
                    drawSceneSprite(batch, ctx, sName, statusX, -12.0, shakeX, shakeY)
                    val slice = GameAtlas.getSprite(sName)
                    statusX += (slice?.width ?: 8) + 1
                }
            }
        }
    }

    private fun drawOrbs(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        centerX: Double,
        y: Double,
        value: Int,
        maxValue: Int,
        sprite: String,
        emptySprite: String,
        shakeX: Double,
        shakeY: Double
    ) {
        val x0 = centerX - (maxValue * 4.0) / 2.0
        for (i in 0 until maxValue) {
            val sName = if (i < value) sprite else emptySprite
            drawSceneSprite(batch, ctx, sName, x0 + i * 4.0, y, shakeX, shakeY)
        }
    }

    private fun drawReticle(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        game: Game,
        shakeX: Double,
        shakeY: Double,
        touchAimActive: Boolean = false
    ) {
        val pt = game.getCastingPoint()
        drawSceneSprite(batch, ctx, "reticle", pt.x - 3.5, pt.y - 3.5, shakeX, shakeY)
    }

    private fun drawHud(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        game: Game,
        isPaused: Boolean
    ) {
        if (game.state == GameState.INTRO || game.state == GameState.WIN) return

        // Norman icon
        drawSprite(batch, ctx, "norman_icon", 2.0, 2.0)

        // Player HP
        for (i in 0 until game.player.maxHp) {
            val sName = if (i < game.player.hp) "health_orb" else "health_orb_empty"
            drawSprite(batch, ctx, sName, (14 + i * 5).toDouble(), 3.0)
        }

        // Spell Casts
        for (i in 0 until game.spell.maxCasts) {
            val sName = if (i < game.spell.casts) "cast_orb" else "cast_orb_empty"
            drawSprite(batch, ctx, sName, (14 + i * 5).toDouble(), 9.0)
        }

        // Souls & streak
        val soulsInt = game.souls.toInt()
        val mult = game.getStreakMultiplier()
        val bonus = if (mult > 0.0) " (+${(mult * 100).roundToInt()}%)" else ""
        write(batch, ctx, "$$soulsInt$bonus", 170.0, 3.0)

        // Level
        write(batch, ctx, "${game.level + 1}-10", 305.0, 14.0)

        // Pause button (mobile & iOS touch-safe: lower down and clearly clickable)
        val pauseBtnX = 346.0
        val pauseBtnY = 8.0
        val pauseBtnW = 46.0
        val pauseBtnH = 18.0
        drawNineSlice(batch, ctx, "pink_frame", pauseBtnX, pauseBtnY, pauseBtnW, pauseBtnH)
        val pauseLabel = if (isPaused) "PLAY" else "PAUSE"
        val labelOffset = if (isPaused) 12.0 else 8.0
        write(batch, ctx, pauseLabel, pauseBtnX + labelOffset, pauseBtnY + 6.0)

        // Resurrect Button (Mobile touch-friendly + Desktop: elevated safely above iOS Home Indicator)
        if (game.state == GameState.PLAYING) {
            val bx = 140.0
            val by = 158.0
            val bw = 120.0
            val bh = 15.0
            val progress = clamp(game.ability.timer / game.ability.cooldown, 0.0, 1.0)
            val fillW = (bw * (1.0 - progress)).toInt()

            drawNineSlice(batch, ctx, "pink_frame", bx, by, fillW.toDouble(), bh)
            drawSprite(batch, ctx, "skull", bx + 2.0, by + 4.0)

            val ready = progress >= 1.0
            val label = if (ready) "RESURRECT (TAP/SPACE)" else "Resurrect (${((1.0 - progress) * game.ability.cooldown / 1000).toInt()}s)"
            write(batch, ctx, label, bx + 12.0, by + 5.0)
        }
    }

    private fun drawPauseOverlay(
        batch: BatchBuilder2D,
        ctx: RenderContext
    ) {
        val modalW = 160.0
        val modalH = 68.0
        val modalX = (400.0 - modalW) / 2.0
        val modalY = (200.0 - modalH) / 2.0

        drawNineSlice(batch, ctx, "pink_frame", modalX, modalY, modalW, modalH)
        write(batch, ctx, "GAME PAUSED", modalX + 46.0, modalY + 14.0)

        val btnW = 96.0
        val btnH = 18.0
        val btnX = modalX + (modalW - btnW) / 2.0
        val btnY = modalY + 34.0
        drawNineSlice(batch, ctx, "pink_frame", btnX, btnY, btnW, btnH)
        write(batch, ctx, "RESUME (TAP)", btnX + 11.0, btnY + 6.0)
    }

    private fun drawShop(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        game: Game
    ) {
        write(batch, ctx, "=== RITUALS SHOP ===", 135.0, 20.0)

        val selected = ShopManager.items.getOrNull(ShopManager.selectedIndex)

        var itemY = 40.0
        for ((index, item) in ShopManager.items.withIndex()) {
            val isSel = index == ShopManager.selectedIndex
            val prefix = if (isSel) "> " else "  "
            val costStr = if (item.cost > 0) "$${item.cost}" else "[FREE]"
            val text = "$prefix${item.name} $costStr"
            write(batch, ctx, text, 100.0, itemY)
            itemY += 12.0
        }

        selected?.let {
            write(batch, ctx, "Desc: ${it.description}", 100.0, itemY + 6.0)
            if (it.cost > 0) {
                val canAfford = it.cost <= game.souls
                val actionText = if (canAfford) "[ TAP HERE TO BUY ]" else "[ NEED MORE SOULS ]"
                write(batch, ctx, actionText, 130.0, itemY + 20.0)
            } else {
                write(batch, ctx, "[ TAP HERE TO BEGIN NEXT WAVE ]", 110.0, itemY + 20.0)
            }
        }
    }

    private fun drawDialogue(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        game: Game
    ) {
        if (game.dialogue.isEmpty()) return
        val currentText = game.dialogue[0]

        if (game.state == GameState.WIN) {
            // Draw centered dialogue / credits text cleanly without background frame
            writeCentered(batch, ctx, currentText, 70.0, lineSpacing = 12.0)

            // Draw tap hint below text
            val hintText = if (game.dialogue.size > 1) "(Tap to continue)" else "(Tap anywhere to play again)"
            val hintW = getTextWidth(hintText)
            write(batch, ctx, hintText, (400.0 - hintW) / 2.0, 110.0)
        } else {
            val textX = 65.0
            val textY = 70.0
            write(batch, ctx, currentText, textX, textY)
            if (game.state == GameState.INTRO) {
                write(batch, ctx, "(Tap screen to skip)", 140.0, 95.0)
            }
        }
    }
}
