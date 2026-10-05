package io.github.kevinah95.norman_the_necromancer.core

import io.github.kevinah95.norman_the_necromancer.assets.GameSprites
import io.github.kevinah95.norman_the_necromancer.fx.Fx
import kotlin.math.abs

enum class GameState {
    INTRO,
    PLAYING,
    SHOPPING,
    LOSE,
    WIN
}

data class Stage(
    var width: Double = GameLayout.VIRTUAL_WIDTH,
    var height: Double = GameLayout.VIRTUAL_HEIGHT,
    var floor: Double = 0.0,
    var ceiling: Double = GameLayout.VIRTUAL_HEIGHT
)

data class SpellState(
    var targetAngle: Double = 0.0,
    var targetRadius: Double = 15.0,
    var basePower: Double = 180.0,
    var shotsPerRound: Int = 1,
    var shotOffsetAngle: Double = 0.1,
    var maxCasts: Int = 3,
    var casts: Int = 3,
    var castRechargeRate: Double = 1000.0,
    var castRechargeTimer: Double = 0.0
)

data class AbilityState(
    var cooldown: Double = 10_000.0,
    var timer: Double = 10_000.0
)

class Game : GameSession {
    val stage: Stage = Stage()
    val objects: MutableList<GameObject> = mutableListOf()
    lateinit var player: GameObject
    val rituals: MutableList<Ritual> = mutableListOf()
    var state: GameState = GameState.INTRO
    var souls: Double = 0.0
    var streak: Int = 0
    var level: Int = 0
    val dialogue: MutableList<String> = mutableListOf()

    val spell: SpellState = SpellState()
    val ability: AbilityState = AbilityState()

    var castAnimationTimer: Double = 0.0
    private var castGroupId: Int = 1

    companion object {
        const val MAX_STREAK: Int = 10
    }

    override fun spawn(gameObject: GameObject, x: Double, y: Double) {
        gameObject.x = x
        gameObject.y = y
        gameObject.gameSession = this
        objects.add(gameObject)
    }

    override fun despawn(gameObject: GameObject) {
        gameObject.emitter?.remove()
        for (b in gameObject.behaviours.toList()) {
            gameObject.removeBehaviour(b)
        }
        objects.remove(gameObject)
    }

    override fun addSouls(amount: Int) {
        souls += amount + amount * getStreakMultiplier()
    }

    fun getStreakMultiplier(): Double = streak.toDouble() / MAX_STREAK.toDouble()

    fun addRitual(ritual: Ritual) {
        ritual.game = this
        rituals.add(ritual)
        ritual.onActive()
    }

    fun canAddRitual(ritual: Ritual): Boolean {
        if (ritual.exclusiveTags != 0) {
            for (other in rituals) {
                if ((ritual.exclusiveTags and other.tags) != 0) return false
            }
        }
        if (ritual.requiredTags != 0) {
            for (other in rituals) {
                if ((ritual.requiredTags and other.tags) != 0) return true
            }
            return false
        }
        return true
    }

    fun getCastingPoint(): Point2D {
        val center = player.center()
        val (vx, vy) = vectorFromAngle(spell.targetAngle)
        return Point2D(
            center.x + vx * spell.targetRadius,
            center.y + vy * spell.targetRadius
        )
    }

    /**
     * Dispatches an action across all active rituals.
     * Takes a snapshot with [toList] to safely allow rituals to attach or detach themselves.
     */
    private inline fun dispatchToRituals(action: (Ritual) -> Unit) {
        for (ritual in rituals.toList()) {
            action(ritual)
        }
    }

    fun onLevelStart() = dispatchToRituals { it.onLevelStart() }

    fun onLevelEnd() = dispatchToRituals { it.onLevelEnd() }

    fun onShopEnter() = dispatchToRituals { it.onShopEnter() }

    fun onCast(spellObj: GameObject, recursive: Boolean = false) {
        dispatchToRituals { ritual ->
            if (!recursive || ritual.recursive) {
                ritual.onCast(spellObj)
            }
        }
    }

    fun update(dtMs: Double) {
        updateAbility(dtMs)
        updateSpell(dtMs)
        updateObjects(dtMs)
        updatePhysics(dtMs)
        updateRituals(dtMs)

        if (castAnimationTimer > 0) {
            castAnimationTimer -= dtMs
            if (castAnimationTimer <= 0) {
                player.spriteName = GameSprites.NORMAN_ARMS_DOWN
            }
        }
    }

    private fun updateAbility(dtMs: Double) {
        ability.timer += dtMs
        player.emitter?.let {
            it.frequency = if (ability.timer >= ability.cooldown) 0.1 else 0.0
        }
    }

    private fun updateSpell(dtMs: Double) {
        if (spell.casts < spell.maxCasts) {
            spell.castRechargeTimer += dtMs
            if (spell.castRechargeTimer >= spell.castRechargeRate) {
                spell.casts += 1
                spell.castRechargeTimer = 0.0
            }
        }
    }

    private fun updateRituals(dtMs: Double) = dispatchToRituals { it.onFrame(dtMs) }

    private fun updateObjects(dtMs: Double) {
        for (obj in objects.toList()) {
            obj.update(dtMs)
        }
    }

    private fun updatePhysics(dtMs: Double) {
        val d = dtMs / 1000.0
        val currentObjects = objects.toList()

        // Phase 1: Position integration & boundary constraints (floor, ceiling, gravity, bounces)
        for (obj in currentObjects) {
            obj.x += obj.vx * d
            obj.y += obj.vy * d

            val lower = stage.floor
            val upper = stage.ceiling - obj.spriteHeight

            if (obj.y < lower || obj.y > upper) {
                obj.y = clamp(obj.y, lower, upper)
                if (abs(obj.vy) >= 10.0) {
                    obj.onBounce()
                }
                obj.vy *= -obj.bounce
            }

            if (obj.y == lower || obj.y == upper) {
                obj.vx *= (1.0 - obj.friction)
            }

            if (obj.mass > 0 && obj.y > 0) {
                obj.vy -= obj.mass * d
            }
        }

        // Phase 2: Inter-object collisions
        for (obj in currentObjects) {
            for (target in currentObjects) {
                if (obj.canCollideWith(target) && obj.bounds().overlaps(target.bounds())) {
                    obj.onCollision(target)
                }
            }
        }
    }

    fun damage(target: GameObject, amount: Int, dealer: GameObject? = null) {
        val dmg = Damage(amount, dealer)
        target.onDamage(dmg)
        target.hp = clamp(target.hp - dmg.amount, 0, target.maxHp)
        if (target.hp <= 0) {
            die(target, dealer)
        }
    }

    fun die(target: GameObject, killer: GameObject? = null) {
        val death = Death(target, killer, target.souls)

        if (target.isTagged(Tags.MOBILE)) {
            val center = target.center()
            Fx.bones(center.x, center.y).burst(2 + randomInt(3)).remove()

            dispatchToRituals { it.onDeath(death) }

            if (randomFloat() <= target.corpseChance) {
                val corpse = io.github.kevinah95.norman_the_necromancer.entities.createCorpse()
                spawn(corpse, center.x, center.y)
            }

            addSouls(death.souls)
        }

        target.onDeath(death)
        despawn(target)
    }

    fun castSpell(): Boolean {
        if (spell.casts <= 0) return false
        spell.casts--

        player.spriteName = GameSprites.NORMAN_ARMS_UP
        castAnimationTimer = 500.0

        val power = spell.basePower
        val targetAngle = spell.targetAngle - (spell.shotsPerRound * spell.shotOffsetAngle / 2.0)
        val groupId = castGroupId++

        for (j in 0 until spell.shotsPerRound) {
            val projectile = io.github.kevinah95.norman_the_necromancer.entities.createSpell()
            val angle = targetAngle + j * spell.shotOffsetAngle
            val (vx, vy) = vectorFromAngle(angle)
            val pt = getCastingPoint()
            projectile.x = pt.x - projectile.spriteWidth / 2.0
            projectile.y = pt.y - projectile.spriteHeight / 2.0
            projectile.vx = vx * power
            projectile.vy = vy * power
            projectile.groupId = groupId
            spawn(projectile)
            onCast(projectile)
        }
        return true
    }

    fun resurrect(): Boolean {
        if (ability.timer < ability.cooldown) return false
        ability.timer = 0.0

        for (ritual in rituals.toList()) {
            ritual.onResurrect()
        }

        val corpses = objects.filter { it.isTagged(Tags.CORPSE) }
        for (corpse in corpses) {
            despawn(corpse)

            val unit = io.github.kevinah95.norman_the_necromancer.entities.createSkeleton()
            spawn(unit, corpse.x, 0.0)
            Fx.resurrect(unit.bounds()).burst(10).remove()

            for (ritual in rituals.toList()) {
                ritual.onResurrection(unit)
            }
        }
        return true
    }
}
