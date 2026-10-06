package io.github.kevinah95.norman_the_necromancer.shop

import io.github.kevinah95.norman_the_necromancer.core.*
import io.github.kevinah95.norman_the_necromancer.levels.LevelManager
import io.github.kevinah95.norman_the_necromancer.rituals.getAllShopRituals
import kotlin.math.pow

object ShopKeys {
    const val HEAL = "Heal"
    const val RENEW = "Renew"
    const val RECHARGE = "Recharge"
    const val CONTINUE = "Continue"
}

data class ShopItem(
    var cost: Int,
    val key: String,
    val onPurchase: () -> Unit,
    val customName: String? = null,
    val customDescription: String? = null
) {
    val name: String
        get() = customName ?: io.github.kevinah95.norman_the_necromancer.i18n.GameStrings.getRitualName(key)

    val description: String
        get() = customDescription ?: io.github.kevinah95.norman_the_necromancer.i18n.GameStrings.getRitualDesc(key)

    /** Secondary constructor for backwards compatibility */
    constructor(
        cost: Int,
        name: String,
        description: String,
        onPurchase: () -> Unit
    ) : this(
        cost = cost,
        key = name,
        onPurchase = onPurchase,
        customName = name,
        customDescription = description
    )
}


object ShopManager {
    var currentGame: Game? = null
    val availableRituals: MutableList<Ritual> = mutableListOf()
    val items: MutableList<ShopItem> = mutableListOf()
    var selectedIndex: Int = 0

    fun init(game: Game) {
        currentGame = game
        availableRituals.clear()
        availableRituals.addAll(getAllShopRituals())
    }

    fun selectIndex(step: Int) {
        if (items.isEmpty()) return
        selectedIndex = clamp(selectedIndex + step, 0, items.size - 1)
    }

    fun buyCurrent(): Boolean {
        val game = currentGame ?: return false
        if (selectedIndex !in 0 until items.size) return false
        val item = items[selectedIndex]
        if (item.cost <= game.souls) {
            game.souls -= item.cost
            items.removeAt(selectedIndex)
            item.onPurchase()
            selectIndex(0)
            return true
        }
        return false
    }

    fun buyItem(index: Int): Boolean {
        selectedIndex = index
        return buyCurrent()
    }

    fun enterShop() {
        val game = currentGame ?: return
        game.state = GameState.SHOPPING
        restockShop()
        game.onShopEnter()
        io.github.kevinah95.norman_the_necromancer.audio.GameAudio.useShopSynths()
    }

    fun exitShop() {
        val game = currentGame ?: return
        game.state = GameState.PLAYING
        LevelManager.nextLevel()
        io.github.kevinah95.norman_the_necromancer.audio.GameAudio.useLevelSynths(game.level)
    }

    fun restockShop() {
        val game = currentGame ?: return
        items.clear()
        val exp = (game.level + 1).toDouble().pow(2).toInt()

        if (game.player.hp < game.player.maxHp) {
            items.add(
                ShopItem(
                    cost = 10 * (game.level + 1),
                    key = ShopKeys.HEAL,
                    onPurchase = { game.damage(game.player, -1) }
                )
            )
        }

        items.add(
            ShopItem(
                cost = 10 * exp,
                key = ShopKeys.RENEW,
                onPurchase = {
                    game.player.maxHp++
                    game.player.hp++
                }
            )
        )

        items.add(
            ShopItem(
                cost = 10 * exp,
                key = ShopKeys.RECHARGE,
                onPurchase = {
                    game.spell.maxCasts++
                    game.spell.casts++
                }
            )
        )

        // Add ritual items
        val candidateRituals = availableRituals.filter { game.canAddRitual(it) }.shuffled()
        val rares = candidateRituals.filter { it.rarity == Rarity.RARE }
        val commons = candidateRituals.filter { it.rarity == Rarity.COMMON }
        val pool = (rares.take(1) + commons.take(2))

        for (ritual in pool) {
            val cost = if (ritual.rarity == Rarity.RARE) 200 + randomInt(100) else 75 + randomInt(100)
            items.add(
                ShopItem(
                    cost = cost,
                    key = ritual.key,
                    onPurchase = {
                        availableRituals.remove(ritual)
                        game.addRitual(ritual)
                    }
                )
            )
        }

        // Continue button item
        items.add(
            ShopItem(
                cost = 0,
                key = ShopKeys.CONTINUE,
                onPurchase = { exitShop() }
            )
        )

        selectedIndex = 0
    }
}

