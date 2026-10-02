package io.github.kevinah95.norman_the_necromancer.fx

import io.github.kevinah95.norman_the_necromancer.core.randomFloat
import io.github.kevinah95.norman_the_necromancer.core.randomRange
import io.github.kevinah95.norman_the_necromancer.core.vectorFromAngle
import kotlin.math.max

data class Particle(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var vx: Double = 0.0,
    var vy: Double = 0.0,
    var bounce: Double = 0.0,
    var elapsed: Double = 0.0,
    var duration: Double = 0.0,
    var variantIndex: Int = 0,
    var mass: Double = 0.0,
    var friction: Double = 0.0
)

data class ParticleRange(val base: Double, val spread: Double) {
    fun sample(): Double = randomRange(base, spread)
}

class ParticleEmitter(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var w: Double = 0.0,
    var h: Double = 0.0,
    var variants: List<List<String>> = emptyList(),
    var frequency: Double = 0.0,
    var velocity: ParticleRange = ParticleRange(0.0, 0.0),
    var angle: ParticleRange = ParticleRange(0.0, 0.0),
    var duration: ParticleRange = ParticleRange(0.0, 0.0),
    var bounce: ParticleRange = ParticleRange(0.0, 0.0),
    var friction: ParticleRange = ParticleRange(0.0, 0.0),
    var mass: ParticleRange = ParticleRange(0.0, 0.0)
) {
    val particles: MutableList<Particle> = mutableListOf()
    private var clock: Double = 0.0
    var done: Boolean = false

    fun remove() {
        done = true
    }

    fun burst(count: Int): ParticleEmitter {
        for (i in 0 until count) {
            emit()
        }
        return this
    }

    fun emit() {
        if (variants.isEmpty()) return
        val vel = velocity.sample()
        val ang = angle.sample()
        val (vx, vy) = vectorFromAngle(ang)
        val p = Particle(
            x = randomRange(x, w),
            y = randomRange(y, h),
            vx = vx * vel,
            vy = vy * vel,
            elapsed = 0.0,
            duration = max(1.0, duration.sample()),
            bounce = bounce.sample(),
            friction = friction.sample(),
            mass = mass.sample(),
            variantIndex = if (variants.isNotEmpty()) (randomFloat() * variants.size).toInt().coerceIn(0, variants.size - 1) else 0
        )
        particles.add(p)
    }

    fun update(dtMs: Double) {
        val t = dtMs / 1000.0
        clock += frequency
        while (!done && clock > 0) {
            clock -= 1.0
            emit()
        }

        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.elapsed += dtMs
            if (p.elapsed >= p.duration) {
                iterator.remove()
            } else {
                p.x += p.vx * t
                p.y += p.vy * t
                p.vy -= p.mass * t

                if (p.y <= 0.0) {
                    p.y = 0.0
                    p.vy = -p.vy * p.bounce
                    p.vx *= (1.0 - p.friction)
                }
            }
        }
    }
}
