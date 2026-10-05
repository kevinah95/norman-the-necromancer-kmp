package io.github.kevinah95.norman_the_necromancer.renderer

import io.github.kevinah95.norman_the_necromancer.assets.GameAtlas
import io.github.kevinah95.norman_the_necromancer.assets.GameSprites
import io.github.kevinah95.norman_the_necromancer.behaviours.Frozen
import io.github.kevinah95.norman_the_necromancer.core.Game
import io.github.kevinah95.norman_the_necromancer.core.GameState
import io.github.kevinah95.norman_the_necromancer.core.clamp
import io.github.kevinah95.norman_the_necromancer.fx.Fx
import korlibs.korge.render.BatchBuilder2D
import korlibs.korge.render.RenderContext

/**
 * Renders the in-game physical world layer:
 * - Background arena environment (floor, ceiling, wall tiles)
 * - Active particle systems and projectile trails
 * - Entities (Norman, enemies, spells, status effects, and health meters)
 * - Targeting reticle
 */
class WorldRenderer(
    private val gfx: GraphicsRenderer
) {
    fun render(
        batch: BatchBuilder2D,
        ctx: RenderContext,
        game: Game,
        shakeX: Double,
        shakeY: Double,
        touchAimActive: Boolean
    ) {
        drawBackground(batch, ctx, game, shakeX, shakeY)
        drawParticles(batch, ctx, shakeX, shakeY)
        drawObjects(batch, ctx, game, shakeX, shakeY)

        if (game.state == GameState.PLAYING) {
            drawReticle(batch, ctx, game, shakeX, shakeY, touchAimActive)
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
            val wallSprite = if (i % 5 != 0) GameSprites.WALL else GameSprites.DOOR
            gfx.drawSceneSprite(batch, ctx, wallSprite, i * 16.0, 0.0, shakeX, shakeY)
            gfx.drawSceneSprite(batch, ctx, GameSprites.FLOOR, i * 16.0, -8.0, shakeX, shakeY)
            gfx.drawSceneSprite(batch, ctx, GameSprites.CEILING, i * 16.0, game.stage.ceiling, shakeX, shakeY)
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
                        gfx.drawSceneSprite(batch, ctx, spriteName, p.x, p.y, shakeX, shakeY)
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
            gfx.drawSceneSprite(batch, ctx, obj.spriteName, obj.x, obj.y + obj.hop, shakeX, shakeY)

            // Frozen overlay
            if (obj.getBehaviour<Frozen>() != null) {
                val slice = GameAtlas.getSprite(obj.spriteName)
                val sw = slice?.width ?: obj.spriteWidth
                val sh = slice?.height ?: obj.spriteHeight
                val screenX = obj.x + shakeX
                val screenY = gfx.sceneOriginY - obj.y - sh + shakeY
                gfx.drawNineSlice(batch, ctx, GameSprites.ICE, screenX, screenY, sw.toDouble(), sh.toDouble())
            }

            // Health orbs
            if (obj.maxHp > 1 && obj !== game.player) {
                if (obj.maxHp < 10) {
                    val cx = obj.center().x
                    gfx.drawOrbs(batch, ctx, cx, -6.0, obj.hp, obj.maxHp, GameSprites.HEALTH_ORB, GameSprites.HEALTH_ORB_EMPTY, shakeX, shakeY)
                } else {
                    gfx.drawSceneSprite(batch, ctx, GameSprites.HEALTH_ORB, obj.x, -6.0, shakeX, shakeY)
                    val screenX = obj.x + 6.0 + shakeX
                    val screenY = gfx.sceneOriginY - 0.0 + shakeY
                    gfx.write(batch, ctx, "${obj.hp}/${obj.maxHp}", screenX, screenY)
                }
            }

            // Status behaviour sprites
            var statusX = obj.x
            for (b in obj.behaviours) {
                val sName = b.spriteName
                if (sName != null) {
                    gfx.drawSceneSprite(batch, ctx, sName, statusX, -12.0, shakeX, shakeY)
                    val slice = GameAtlas.getSprite(sName)
                    statusX += (slice?.width ?: 8) + 1
                }
            }
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
        gfx.drawSceneSprite(batch, ctx, GameSprites.RETICLE, pt.x - 3.5, pt.y - 3.5, shakeX, shakeY)
    }
}
