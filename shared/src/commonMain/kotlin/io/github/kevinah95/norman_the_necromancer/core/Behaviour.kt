package io.github.kevinah95.norman_the_necromancer.core

open class Behaviour(var gameObject: GameObject) {
    var turns: Int = 1
    var timer: Int = 0
    open var spriteName: String? = null

    open fun onAdded() {}
    open fun onRemoved() {}

    /**
     * Called when the turn timer expires. Return true to break behaviour execution chain.
     */
    open fun onUpdate(): Boolean = false

    open fun onBounce() {}
    open fun onDamage(damage: Damage) {}
    open fun onDeath(death: Death) {}
    open fun onFrame(dt: Double) {}
    open fun onCollision(target: GameObject) {}
}
