package io.github.kevinah95.norman_the_necromancer.core

import io.github.kevinah95.norman_the_necromancer.fx.ParticleEmitter

interface GameSession {
    fun spawn(gameObject: GameObject, x: Double = gameObject.x, y: Double = gameObject.y)
    fun despawn(gameObject: GameObject)
    fun addSouls(amount: Int)
}

open class GameObject {
    // Physics
    var x: Double = 0.0
    var y: Double = 0.0
    var vx: Double = 0.0
    var vy: Double = 0.0
    var mass: Double = 0.0
    var bounce: Double = 0.0
    var friction: Double = 0.0
    var hop: Double = 0.0

    // Display
    var spriteName: String = ""
    var spriteWidth: Int = 16
    var spriteHeight: Int = 16
    var emitter: ParticleEmitter? = null

    // Logic
    var tags: Int = 0
    var collisionMask: Int = 0
    var hp: Int = 0
    var maxHp: Int = 0
    var souls: Int = 0
    var corpseChance: Double = 0.0
    var despawnOnCollision: Boolean = false
    var despawnOnBounce: Boolean = false
    var groupId: Int = 0

    // Behaviours
    val behaviours: MutableList<Behaviour> = mutableListOf()
    var updateSpeed: Double = 0.0
    var updateClock: Double = 0.0

    // Callbacks
    var onCollisionAction: ((GameObject) -> Unit)? = null
    var onDeathAction: ((Death) -> Unit)? = null
    var onDamageAction: ((Damage) -> Unit)? = null
    var onBounceAction: (() -> Unit)? = null

    var gameSession: GameSession? = null

    fun isTagged(mask: Int): Boolean = (tags and mask) != 0

    /**
     * Determines whether this object should evaluate physical collision with [target],
     * checking that they are distinct entities and that [target]'s tags match our [collisionMask].
     */
    fun canCollideWith(target: GameObject): Boolean =
        this !== target && (collisionMask and target.tags) != 0

    fun bounds(): Rect2D = Rect2D(x, y, spriteWidth.toDouble(), spriteHeight.toDouble())

    fun center(): Point2D = Point2D(x + spriteWidth / 2.0, y + spriteHeight / 2.0)

    fun update(dtMs: Double) {
        onFrame(dtMs)

        updateClock -= dtMs
        if (updateClock <= 0 && updateSpeed > 0) {
            updateClock = updateSpeed
            onUpdate()
        }

        emitter?.let {
            it.x = x
            it.y = y
        }
    }

    fun addBehaviour(behaviour: Behaviour, index: Int = behaviours.size): Behaviour {
        behaviour.gameObject = this
        val existing = behaviours.firstOrNull { it::class == behaviour::class }
        if (behaviour::class != Behaviour::class && existing != null) {
            return existing
        }
        if (index in 0..behaviours.size) {
            behaviours.add(index, behaviour)
        } else {
            behaviours.add(behaviour)
        }
        behaviour.onAdded()
        return behaviour
    }

    fun removeBehaviour(behaviour: Behaviour) {
        if (behaviours.remove(behaviour)) {
            behaviour.onRemoved()
        }
    }

    inline fun <reified T : Behaviour> getBehaviour(): T? {
        return behaviours.filterIsInstance<T>().firstOrNull()
    }

    /**
     * Helper to dispatch an event to all attached behaviours.
     * Takes a snapshot with [toList] to safely allow behaviours to attach or remove
     * behaviours during event handling without ConcurrentModificationException.
     */
    private inline fun dispatchToBehaviours(action: (Behaviour) -> Unit) {
        for (b in behaviours.toList()) {
            action(b)
        }
    }

    fun onFrame(dtMs: Double) = dispatchToBehaviours { it.onFrame(dtMs) }

    fun onUpdate() {
        // Explicit loop because returning true acts as a circuit breaker
        // (e.g. Frozen stops subsequent behaviours from ticking this turn).
        for (b in behaviours.toList()) {
            b.timer++
            if (b.timer >= b.turns) {
                b.timer = 0
                if (b.onUpdate()) break
            }
        }
    }

    fun onDamage(damage: Damage) {
        onDamageAction?.invoke(damage)
        dispatchToBehaviours { it.onDamage(damage) }
    }

    fun onDeath(death: Death) {
        onDeathAction?.invoke(death)
        dispatchToBehaviours { it.onDeath(death) }
    }

    fun onBounce() {
        onBounceAction?.invoke()
        dispatchToBehaviours { it.onBounce() }
        if (despawnOnBounce) {
            gameSession?.despawn(this)
        }
    }

    fun onCollision(target: GameObject) {
        onCollisionAction?.invoke(target)
        dispatchToBehaviours { it.onCollision(target) }
        if (despawnOnCollision) {
            gameSession?.despawn(this)
        }
    }
}
