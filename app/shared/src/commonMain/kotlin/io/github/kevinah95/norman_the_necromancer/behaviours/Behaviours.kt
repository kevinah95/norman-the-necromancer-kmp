package io.github.kevinah95.norman_the_necromancer.behaviours

import io.github.kevinah95.norman_the_necromancer.assets.GameSprites
import io.github.kevinah95.norman_the_necromancer.core.*
import io.github.kevinah95.norman_the_necromancer.entities.createLightningSpell
import io.github.kevinah95.norman_the_necromancer.fx.Fx
import io.github.kevinah95.norman_the_necromancer.fx.ParticleRange
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

class Attack(gameObject: GameObject) : Behaviour(gameObject) {
    override fun onCollision(target: GameObject) {
        val dealDamage = gameObject.hp
        val takeDamage = target.hp
        val session = gameObject.gameSession as? Game ?: return
        session.damage(target, dealDamage, gameObject)
        session.damage(gameObject, takeDamage, target)
    }
}

class DespawnTimer(gameObject: GameObject, val duration: Double) : Behaviour(gameObject) {
    private var elapsed: Double = 0.0

    override fun onFrame(dt: Double) {
        elapsed += dt
        if (elapsed >= duration) {
            gameObject.gameSession?.despawn(gameObject)
        }
    }
}

class March(gameObject: GameObject, var step: Double) : Behaviour(gameObject) {
    override fun onUpdate(): Boolean {
        if (gameObject.y > 0) return false

        val initialX = gameObject.x
        val targetX = gameObject.x + step
        TweenManager.tween(initialX, targetX, 200.0, korlibs.math.interpolation.Easing.SMOOTH) { x, t ->
            gameObject.x = x
            gameObject.hop = sin(t * PI) * 2.0
            if (t == 1.0 && gameObject.mass >= 100.0) {
                TweenManager.screenshake(50.0)
            }
        }

        val game = gameObject.gameSession as? Game
        val stageWidth = game?.stage?.width ?: GameLayout.VIRTUAL_WIDTH
        if ((step < 0 && gameObject.x < 0) || (step > 0 && gameObject.x > stageWidth)) {
            gameObject.gameSession?.despawn(gameObject)
        }
        return false
    }
}

class Damaging(gameObject: GameObject, var amount: Int = 1) : Behaviour(gameObject) {
    override fun onCollision(target: GameObject) {
        val session = gameObject.gameSession as? Game ?: return
        session.damage(target, amount, gameObject)
    }
}

class Bleeding(gameObject: GameObject) : Behaviour(gameObject) {
    override var spriteName: String? = GameSprites.STATUS_BLEEDING
    init { turns = 3 }
    var amount: Int = 1

    private val emitter = Fx.blood()

    override fun onUpdate(): Boolean {
        val center = gameObject.center()
        emitter.x = center.x
        emitter.y = center.y
        emitter.burst(1)
        val session = gameObject.gameSession as? Game
        session?.damage(gameObject, 1, gameObject)
        return false
    }
}

class Enraged(gameObject: GameObject, val mask: Int) : Behaviour(gameObject) {
    override var spriteName: String? = GameSprites.STATUS_ENRAGED

    private val emitter = Fx.blood()

    override fun onDamage(damage: Damage) {
        if (damage.dealer != null && damage.dealer.isTagged(mask)) {
            val session = gameObject.gameSession as? Game
            session?.damage(gameObject, -damage.amount, gameObject)
            damage.amount = 0
            val b = gameObject.bounds()
            emitter.x = b.x
            emitter.y = b.y
            emitter.w = b.w
            emitter.h = b.h
            emitter.burst(4)
        }
    }
}

class Seeking(gameObject: GameObject) : Behaviour(gameObject) {
    override fun onFrame(dt: Double) {
        val game = gameObject.gameSession as? Game ?: return
        var target: GameObject? = null
        var minDist = 100.0

        val projectilePt = Point2D(gameObject.x, gameObject.y)
        for (obj in game.objects) {
            if (obj.isTagged(gameObject.collisionMask)) {
                val dist = projectilePt.distanceTo(Point2D(obj.x, obj.y))
                if (dist < minDist) {
                    target = obj
                    minDist = dist
                }
            }
        }

        target?.let {
            val currentAngle = vectorToAngle(gameObject.vx, gameObject.vy)
            val desiredAngle = projectilePt.angleTo(it.center())
            val angle = currentAngle + (desiredAngle - currentAngle) / 20.0
            val magnitude = hypot(gameObject.vx, gameObject.vy)
            val (vx, vy) = vectorFromAngle(angle)
            gameObject.vx = vx * magnitude
            gameObject.vy = vy * magnitude
        }
    }
}

class Summon(
    gameObject: GameObject,
    private val create: () -> GameObject,
    private val summonSpeed: Double
) : Behaviour(gameObject) {
    private var summonTimer: Double = 0.0
    var summonCounter: Int = 0
    var onSummonAction: ((GameObject) -> Unit)? = null

    override fun onFrame(dt: Double) {
        summonTimer += dt
        if (summonTimer > summonSpeed) {
            summonTimer = 0.0
            summonCounter++
            val unit = create()
            gameObject.gameSession?.spawn(unit, gameObject.x, gameObject.y)
            onSummonAction?.invoke(unit)
        }
    }
}

data class SpellCounter(var total: Int = 0, var hits: Int = 0)

class HitStreak(gameObject: GameObject) : Behaviour(gameObject) {
    companion object {
        val counters: MutableMap<Int, SpellCounter> = mutableMapOf()
    }

    private var hit = false
    private var counter: SpellCounter? = null

    override fun onCollision(target: GameObject) {
        hit = true
    }

    override fun onAdded() {
        val c = counters.getOrPut(gameObject.groupId) { SpellCounter() }
        c.total++
        counter = c
    }

    override fun onRemoved() {
        val c = counter ?: return
        if (hit) c.hits++
        c.total--
        if (c.total <= 0) {
            counters.remove(gameObject.groupId)
            val game = gameObject.gameSession as? Game ?: return
            if (c.hits > 0) {
                game.streak = clamp(game.streak + 1, 0, Game.MAX_STREAK)
            } else {
                game.streak = 0
            }
        }
    }
}

class Invulnerable(gameObject: GameObject) : Behaviour(gameObject) {
    override var spriteName: String? = GameSprites.STATUS_SHIELDED

    override fun onDamage(damage: Damage) {
        if (damage.amount > 0) {
            damage.amount = 0
        }
    }
}

class Frozen(gameObject: GameObject) : Behaviour(gameObject) {
    var freezeTimer: Int = 10

    override fun onUpdate(): Boolean {
        freezeTimer--
        if (freezeTimer <= 0) {
            gameObject.removeBehaviour(this)
        }
        return true // Prevent subsequent behaviours from updating
    }
}

class LightningStrike(gameObject: GameObject) : Behaviour(gameObject) {
    override fun onCollision(target: GameObject) {
        val game = gameObject.gameSession as? Game ?: return
        val bolts = 3
        for (i in 0 until bolts) {
            val bolt = createLightningSpell()
            bolt.vy = -200.0
            bolt.vx = (randomInt(20) - 10).toDouble()
            bolt.y = clamp((50 + randomInt(100)).toDouble(), 0.0, game.stage.ceiling - 10.0)
            bolt.x = target.x + (randomInt(50) - 25)
            game.spawn(bolt)
        }
    }
}
