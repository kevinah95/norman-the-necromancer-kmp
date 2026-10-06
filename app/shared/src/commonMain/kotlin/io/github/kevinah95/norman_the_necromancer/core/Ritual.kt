package io.github.kevinah95.norman_the_necromancer.core

import io.github.kevinah95.norman_the_necromancer.i18n.GameStrings

enum class Rarity {
    COMMON,
    RARE
}

open class Ritual(
    val key: String,
    val tags: Int = 0,
    val exclusiveTags: Int = 0,
    val requiredTags: Int = 0,
    val recursive: Boolean = true,
    val rarity: Rarity = Rarity.COMMON,
    customName: String? = null,
    customDescription: String? = null
) {
    private val explicitName = customName
    private val explicitDescription = customDescription

    /** Dynamic localized name based on current language or explicit override */
    val name: String
        get() = explicitName ?: GameStrings.getRitualName(key)

    /** Dynamic localized description based on current language or explicit override */
    val description: String
        get() = explicitDescription ?: GameStrings.getRitualDesc(key)

    /** Secondary constructor for backwards compatibility with tests and custom rituals */
    constructor(
        name: String,
        description: String,
        tags: Int = 0,
        exclusiveTags: Int = 0,
        requiredTags: Int = 0,
        recursive: Boolean = true,
        rarity: Rarity = Rarity.COMMON
    ) : this(
        key = name,
        tags = tags,
        exclusiveTags = exclusiveTags,
        requiredTags = requiredTags,
        recursive = recursive,
        rarity = rarity,
        customName = name,
        customDescription = description
    )

    /**
     * Reference to the active [Game] session attached when the ritual is equipped.
     */
    var game: Game? = null

    /**
     * Resolves the active [Game] session from this ritual's [game] reference,
     * falling back to the GameObject's session if available.
     */
    protected fun resolveGame(fallbackObj: GameObject? = null): Game? =
        game ?: (fallbackObj?.gameSession as? Game)

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


