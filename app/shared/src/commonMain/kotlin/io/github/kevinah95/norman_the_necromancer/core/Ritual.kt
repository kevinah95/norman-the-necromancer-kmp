package io.github.kevinah95.norman_the_necromancer.core

enum class Rarity {
    COMMON,
    RARE
}

open class Ritual(
    val name: String,
    val description: String,
    val tags: Int = 0,
    val exclusiveTags: Int = 0,
    val requiredTags: Int = 0,
    val recursive: Boolean = true,
    val rarity: Rarity = Rarity.COMMON
) {
    /**
     * Reference to the active [Game] session attached when the ritual is equipped.
     */
    var game: Game? = null

    open fun onFrame(dtMs: Double) {}
    open fun onActive() {}
    open fun onCast(spell: GameObject) {}
    open fun onResurrect() {}
    open fun onResurrection(gameObject: GameObject) {}
    open fun onDeath(death: Death) {}
    open fun onLevelEnd() {}
    open fun onLevelStart() {}
    open fun onShopEnter() {}
}
