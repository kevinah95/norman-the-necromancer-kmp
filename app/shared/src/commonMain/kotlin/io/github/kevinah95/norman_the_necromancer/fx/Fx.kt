package io.github.kevinah95.norman_the_necromancer.fx

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
                    listOf("p_bone_1"),
                    listOf("p_bone_2"),
                    listOf("p_bone_3")
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
                    listOf("p_green_1", "p_green_2", "p_green_3"),
                    listOf("p_green_2", "p_green_3", "p_green_4"),
                    listOf("p_green_1", "p_green_2", "p_green_3")
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
                    listOf("p_star_1", "p_star_2", "p_star_3"),
                    listOf("p_star_2", "p_star_3", "p_star_4"),
                    listOf("p_star_1", "p_star_3")
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
                    listOf("p_dust_1", "p_dust_2"),
                    listOf("p_dust_2", "p_dust_1", "p_dust_3", "p_dust_1")
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
                    listOf("p_green_1", "p_green_2", "p_green_3"),
                    listOf("p_green_2", "p_green_3", "p_green_4"),
                    listOf("p_green_1", "p_green_3", "p_green_5")
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
                    listOf("health_orb", "health_pip"),
                    listOf("health_pip")
                )
            )
        )
    }

    fun holy(area: Rect2D): ParticleEmitter {
        return cloud(
            area,
            listOf(
                listOf("p_star_1", "p_star_2", "p_star_3"),
                listOf("p_star_2", "p_star_3", "p_star_4"),
                listOf("p_star_1", "p_star_3")
            )
        )
    }

    fun portal(area: Rect2D): ParticleEmitter {
        return cloud(
            area,
            listOf(
                listOf("p_blue_1", "p_blue_2", "p_blue_3"),
                listOf("p_blue_2", "p_blue_3"),
                listOf("p_blue_3")
            )
        ).apply { frequency = 0.2 }
    }
}
