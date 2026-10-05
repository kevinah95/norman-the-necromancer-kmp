package io.github.kevinah95.norman_the_necromancer.renderer

import io.github.kevinah95.norman_the_necromancer.GameVersion
import io.github.kevinah95.norman_the_necromancer.assets.GameAtlas
import io.github.kevinah95.norman_the_necromancer.assets.GameSprites
import io.github.kevinah95.norman_the_necromancer.core.Game
import io.github.kevinah95.norman_the_necromancer.core.GameLayout
import io.github.kevinah95.norman_the_necromancer.core.GameState
import io.github.kevinah95.norman_the_necromancer.core.clamp
import io.github.kevinah95.norman_the_necromancer.i18n.GameStrings
import io.github.kevinah95.norman_the_necromancer.shop.ShopManager
import korlibs.image.color.Colors
import korlibs.korge.render.BatchBuilder2D
import korlibs.korge.render.RenderContext
import kotlin.math.roundToInt

/**
 * Renders the user interface overlay layer:
 * - HUD (Player health, spell casts, souls/streak counter, level badge, pause & resurrect buttons)
 * - Inter-wave ritual shop interface
 * - Dialogue sequences (intro exposition and victory credits)
 * - Pause dialog overlay
 * - Version watermark
 */
class UiRenderer(
    private val gfx: GraphicsRenderer
) {
    fun render(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        game: Game,
        isPaused: Boolean
    ) {
        drawHud(batch, ctx, game, isPaused)

        if (game.state == GameState.SHOPPING) {
            drawShop(batch, ctx, game)
        }

        drawDialogue(batch, ctx, game)

        if (isPaused) {
            drawPauseOverlay(batch, ctx)
        }

        drawVersion(batch, ctx)
    }

    private fun drawVersion(
        batch: BatchBuilder2D,
        ctx: RenderContext
    ) {
        val versionText = "v${GameVersion.VERSION_NAME}"
        val textW = gfx.getTextWidth(versionText)
        val x = GameLayout.VIRTUAL_WIDTH - textW - 4.0
        val y = GameLayout.VIRTUAL_HEIGHT - GameAtlas.GLYPH_HEIGHT - 3.0
        gfx.write(batch, ctx, versionText, x, y, colorMul = Colors.WHITE.withAd(0.30))
    }

    private fun drawHud(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        game: Game,
        isPaused: Boolean
    ) {
        if (game.state == GameState.INTRO || game.state == GameState.WIN) return

        // Norman portrait icon
        gfx.drawSprite(batch, ctx, GameSprites.NORMAN_ICON, 2.0, 2.0)

        // Player HP orbs
        for (i in 0 until game.player.maxHp) {
            val sName = if (i < game.player.hp) GameSprites.HEALTH_ORB else GameSprites.HEALTH_ORB_EMPTY
            gfx.drawSprite(batch, ctx, sName, (14 + i * 5).toDouble(), 3.0)
        }

        // Spell Cast orbs
        for (i in 0 until game.spell.maxCasts) {
            val sName = if (i < game.spell.casts) GameSprites.CAST_ORB else GameSprites.CAST_ORB_EMPTY
            gfx.drawSprite(batch, ctx, sName, (14 + i * 5).toDouble(), 9.0)
        }

        // Souls & streak multiplier
        val soulsInt = game.souls.toInt()
        val mult = game.getStreakMultiplier()
        val bonus = if (mult > 0.0) " (+${(mult * 100).roundToInt()}%)" else ""
        gfx.write(batch, ctx, "$$soulsInt$bonus", 170.0, 3.0)

        // Level indicator
        gfx.write(batch, ctx, "${game.level + 1}-10", 305.0, 14.0)

        // Pause button
        val pauseLabel = if (isPaused) GameStrings.hudPlay else GameStrings.hudPause
        gfx.drawButton(batch, ctx, GameLayout.PAUSE_BUTTON, pauseLabel)

        // Resurrect action button with recharge cooldown bar
        if (game.state == GameState.PLAYING) {
            val bounds = GameLayout.RESURRECT_BUTTON
            val progress = clamp(game.ability.timer / game.ability.cooldown, 0.0, 1.0)
            val fillW = (bounds.w * (1.0 - progress)).toInt()

            gfx.drawNineSlice(batch, ctx, GameSprites.PINK_FRAME, bounds.x, bounds.y, fillW.toDouble(), bounds.h)
            gfx.drawSprite(batch, ctx, GameSprites.SKULL, bounds.x + 2.0, bounds.y + 4.0)

            val ready = progress >= 1.0
            val label = if (ready) GameStrings.hudResurrectReady else GameStrings.hudResurrectCooldown(((1.0 - progress) * game.ability.cooldown / 1000).toInt())
            gfx.write(batch, ctx, label, bounds.x + 12.0, bounds.y + 5.0)
        }
    }

    private fun drawPauseOverlay(
        batch: BatchBuilder2D,
        ctx: RenderContext
    ) {
        val modal = GameLayout.PAUSE_MODAL
        gfx.drawNineSlice(batch, ctx, GameSprites.PINK_FRAME, modal.x, modal.y, modal.w, modal.h)
        gfx.writeCenteredHorizontally(batch, ctx, GameStrings.pauseTitle, modal.y + 14.0, modal.x, modal.w)
        gfx.drawButton(batch, ctx, GameLayout.RESUME_BUTTON, GameStrings.pauseResume)
    }

    private fun drawShop(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        game: Game
    ) {
        gfx.writeCenteredHorizontally(batch, ctx, GameStrings.shopTitle, 20.0)

        val selected = ShopManager.items.getOrNull(ShopManager.selectedIndex)

        var itemY = GameLayout.SHOP_ITEM_START_Y
        for ((index, item) in ShopManager.items.withIndex()) {
            val isSel = index == ShopManager.selectedIndex
            val prefix = if (isSel) "> " else "  "
            val costStr = if (item.cost > 0) "$${item.cost}" else GameStrings.shopFree
            val itemName = GameStrings.getRitualName(item.name)
            val text = "$prefix$itemName $costStr"
            gfx.write(batch, ctx, text, GameLayout.SHOP_ITEM_LIST_X, itemY)
            itemY += GameLayout.SHOP_ITEM_HEIGHT
        }

        selected?.let {
            val desc = GameStrings.getRitualDesc(it.name, it.description)
            gfx.write(batch, ctx, "${GameStrings.shopDesc} $desc", GameLayout.SHOP_ITEM_LIST_X, itemY + 6.0)
            val actionText = if (it.cost > 0) {
                if (it.cost <= game.souls) GameStrings.shopTapBuy else GameStrings.shopNeedSouls
            } else {
                GameStrings.shopNextWave
            }
            gfx.writeCenteredHorizontally(batch, ctx, actionText, itemY + 20.0)
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
            gfx.writeCentered(batch, ctx, currentText, 66.0, lineSpacing = 11.0)

            val hintText = if (game.dialogue.size > 1) GameStrings.outroTapContinue else GameStrings.outroTapPlayAgain
            gfx.writeCenteredHorizontally(batch, ctx, hintText, 116.0)
        } else {
            gfx.writeCenteredHorizontally(batch, ctx, currentText.trim(), 70.0)
            if (game.state == GameState.INTRO) {
                val contText = if (game.dialogue.size > 1) {
                    GameStrings.introTapContinue
                } else {
                    GameStrings.introTapToBegin
                }
                gfx.writeCenteredHorizontally(batch, ctx, contText, 95.0)

                // Language toggle button at the top-right of intro screen
                gfx.drawButton(batch, ctx, GameLayout.LANG_BUTTON, GameStrings.langButtonText)
            }
        }
    }
}
