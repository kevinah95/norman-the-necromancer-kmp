package io.github.kevinah95.norman_the_necromancer.fx

import io.github.kevinah95.norman_the_necromancer.assets.GameSprites
import io.github.kevinah95.norman_the_necromancer.core.*

object Fx {
    val activeEmitters: MutableList<ParticleEmitter> = mutableListOf()

    fun update(dtMs: Double) {
        val iterator = activeEmitters.iterator()
        while (iterator.hasNext()) {
            val emitter = iterator.next()
            emitter.update(dtMs)
            if (emitter.done && emitter.particles.isEmpty()) {
                iterator.remove()
            }
        }
    }

    fun register(emitter: ParticleEmitter): ParticleEmitter {
        activeEmitters.add(emitter)
        return emitter
    }

    fun bones(x: Double, y: Double): ParticleEmitter {
        return register(
            ParticleEmitter(
                x = x,
                y = y,
                duration = ParticleRange(10_000.0, 5_000.0),
                friction = ParticleRange(0.6, 0.0),
                velocity = ParticleRange(5.0, 20.0),
                angle = ParticleRange(DEG_90 - 0.5, 1.0),
                bounce = ParticleRange(0.1, 0.5),
                mass = ParticleRange(60.0, 0.0),
                variants = listOf(
                    listOf(GameSprites.P_BONE_1),
                    listOf(GameSprites.P_BONE_2),
                    listOf(GameSprites.P_BONE_3)
                )
            )
        )
    }

    fun trail(): ParticleEmitter {
        return register(
            ParticleEmitter(
                duration = ParticleRange(500.0, 1000.0),
                velocity = ParticleRange(1.0, 10.0),
                angle = ParticleRange(DEG_180, -0.5),
                bounce = ParticleRange(0.0, 0.0),
                frequency = 2.0,
                mass = ParticleRange(3.0, 0.0),
                friction = ParticleRange(0.5, 0.0),
                variants = listOf(
                    listOf(GameSprites.P_GREEN_1, GameSprites.P_GREEN_2, GameSprites.P_GREEN_3),
                    listOf(GameSprites.P_GREEN_2, GameSprites.P_GREEN_3, GameSprites.P_GREEN_4),
                    listOf(GameSprites.P_GREEN_1, GameSprites.P_GREEN_2, GameSprites.P_GREEN_3)
                )
            )
        )
    }

    fun cloud(area: Rect2D, variants: List<List<String>>): ParticleEmitter {
        return register(
            ParticleEmitter(
                x = area.x,
                y = area.y,
                w = area.w,
                h = area.h,
                duration = ParticleRange(500.0, 1000.0),
                velocity = ParticleRange(1.0, 10.0),
                angle = ParticleRange(DEG_90 - 0.2, 0.4),
                bounce = ParticleRange(0.0, 0.0),
                frequency = 2.0,
                mass = ParticleRange(-2.0, 0.0),
                variants = variants
            )
        )
    }

    fun royalty(): ParticleEmitter {
        return register(
            ParticleEmitter(
                duration = ParticleRange(500.0, 1000.0),
                velocity = ParticleRange(1.0, 10.0),
                angle = ParticleRange(DEG_90, 0.5),
                bounce = ParticleRange(0.0, 0.0),
                frequency = 0.5,
                mass = ParticleRange(3.0, 0.0),
                friction = ParticleRange(0.5, 0.0),
                variants = listOf(
                    listOf(GameSprites.P_STAR_1, GameSprites.P_STAR_2, GameSprites.P_STAR_3),
                    listOf(GameSprites.P_STAR_2, GameSprites.P_STAR_3, GameSprites.P_STAR_4),
                    listOf(GameSprites.P_STAR_1, GameSprites.P_STAR_3)
                )
            )
        )
    }

    fun dust(stageWidth: Double, stageHeight: Double): ParticleEmitter {
        return register(
            ParticleEmitter(
                x = 0.0,
                y = 0.0,
                w = stageWidth,
                h = stageHeight,
                angle = ParticleRange(0.0, DEG_360),
                duration = ParticleRange(5000.0, 10000.0),
                velocity = ParticleRange(1.0, 3.0),
                bounce = ParticleRange(0.0, 0.0),
                frequency = 0.1,
                variants = listOf(
                    listOf(GameSprites.P_DUST_1, GameSprites.P_DUST_2),
                    listOf(GameSprites.P_DUST_2, GameSprites.P_DUST_1, GameSprites.P_DUST_3, GameSprites.P_DUST_1)
                )
            )
        )
    }

    fun resurrect(area: Rect2D): ParticleEmitter {
        return register(
            ParticleEmitter(
                x = area.x,
                y = area.y,
                w = area.w,
                h = area.h,
                duration = ParticleRange(500.0, 1000.0),
                velocity = ParticleRange(1.0, 10.0),
                angle = ParticleRange(DEG_90 - 0.2, 0.4),
                bounce = ParticleRange(0.0, 0.0),
                frequency = 0.0,
                mass = ParticleRange(-2.0, 0.0),
                variants = listOf(
                    listOf(GameSprites.P_GREEN_1, GameSprites.P_GREEN_2, GameSprites.P_GREEN_3),
                    listOf(GameSprites.P_GREEN_2, GameSprites.P_GREEN_3, GameSprites.P_GREEN_4),
                    listOf(GameSprites.P_GREEN_1, GameSprites.P_GREEN_3, GameSprites.P_GREEN_5)
                )
            )
        )
    }

    fun blood(area: Rect2D = Rect2D()): ParticleEmitter {
        return register(
            ParticleEmitter(
                x = area.x,
                y = area.y,
                w = area.w,
                h = area.h,
                duration = ParticleRange(500.0, 1000.0),
                velocity = ParticleRange(10.0, 30.0),
                angle = ParticleRange(DEG_90 - 0.2, 0.4),
                bounce = ParticleRange(0.0, 0.0),
                frequency = 0.0,
                mass = ParticleRange(10.0, 30.0),
                variants = listOf(
                    listOf(GameSprites.HEALTH_ORB, GameSprites.HEALTH_PIP),
                    listOf(GameSprites.HEALTH_PIP)
                )
            )
        )
    }

    fun holy(area: Rect2D): ParticleEmitter {
        return cloud(
            area,
            listOf(
                listOf(GameSprites.P_STAR_1, GameSprites.P_STAR_2, GameSprites.P_STAR_3),
                listOf(GameSprites.P_STAR_2, GameSprites.P_STAR_3, GameSprites.P_STAR_4),
                listOf(GameSprites.P_STAR_1, GameSprites.P_STAR_3)
            )
        )
    }

    fun portal(area: Rect2D): ParticleEmitter {
        return cloud(
            area,
            listOf(
                listOf(GameSprites.P_BLUE_1, GameSprites.P_BLUE_2, GameSprites.P_BLUE_3),
                listOf(GameSprites.P_BLUE_2, GameSprites.P_BLUE_3),
                listOf(GameSprites.P_BLUE_3)
            )
        ).apply { frequency = 0.2 }
    }
}
