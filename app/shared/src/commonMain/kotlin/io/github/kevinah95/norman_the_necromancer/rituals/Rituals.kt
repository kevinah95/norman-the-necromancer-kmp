package io.github.kevinah95.norman_the_necromancer.rituals

import io.github.kevinah95.norman_the_necromancer.assets.GameSprites
import io.github.kevinah95.norman_the_necromancer.behaviours.*
import io.github.kevinah95.norman_the_necromancer.core.*
import io.github.kevinah95.norman_the_necromancer.entities.createSkeletonLord
import io.github.kevinah95.norman_the_necromancer.entities.createSpell
import io.github.kevinah95.norman_the_necromancer.fx.Fx
import io.github.kevinah95.norman_the_necromancer.fx.ParticleRange
import io.github.kevinah95.norman_the_necromancer.i18n.RitualKeys
import io.github.kevinah95.norman_the_necromancer.shop.ShopManager
import korlibs.math.interpolation.Easing

/**
 * Bitmask tags used for ritual categorization, dependencies, and exclusions.
 */
object RitualTags {
    const val NONE: Int = 0
    const val BOUNCING: Int = 1 shl 0
    const val SPLITTING: Int = 1 shl 1
    const val EXPLOSIVE: Int = 1 shl 2
    const val HOMING: Int = 1 shl 3
    const val WARDSTONES: Int = 1 shl 4
    const val CASTING_RATE: Int = 1 shl 5
    const val CURSE: Int = 1 shl 6
}

// Backwards-compatible top-level constants
const val TAG_NONE: Int = RitualTags.NONE
const val TAG_BOUNCING: Int = RitualTags.BOUNCING
const val TAG_SPLITTING: Int = RitualTags.SPLITTING
const val TAG_EXPLOSIVE: Int = RitualTags.EXPLOSIVE
const val TAG_HOMING: Int = RitualTags.HOMING
const val TAG_WARDSTONES: Int = RitualTags.WARDSTONES
const val TAG_CASTING_RATE: Int = RitualTags.CASTING_RATE
const val TAG_CURSE: Int = RitualTags.CURSE

// --- Concrete Ritual Singletons ---

object StreakRitual : Ritual(
    key = RitualKeys.STREAK,
    tags = RitualTags.NONE
) {
    override fun onCast(spell: GameObject) {
        spell.addBehaviour(HitStreak(spell))
    }
}

object BouncingRitual : Ritual(
    key = RitualKeys.BOUNCING,
    tags = RitualTags.BOUNCING
) {
    override fun onCast(spell: GameObject) {
        spell.addBehaviour(DespawnTimer(spell, 3000.0))
        spell.despawnOnBounce = false
        spell.bounce = 0.5
    }
}

object DoubleshotRitual : Ritual(
    key = RitualKeys.DOUBLESHOT,
    tags = RitualTags.SPLITTING,
    exclusiveTags = RitualTags.SPLITTING,
    rarity = Rarity.RARE
) {
    override fun onActive() {
        resolveGame()?.spell?.shotsPerRound = 2
    }
}

object HunterRitual : Ritual(
    key = RitualKeys.HUNTER,
    tags = RitualTags.HOMING,
    rarity = Rarity.RARE
) {
    override fun onCast(spell: GameObject) {
        spell.addBehaviour(Seeking(spell))
    }
}

object WeightlessRitual : Ritual(
    key = RitualKeys.WEIGHTLESS,
    tags = RitualTags.NONE
) {
    override fun onCast(spell: GameObject) {
        spell.mass = 0.0
        spell.friction = 0.0
        spell.bounce = 1.0
    }
}

object KnockbackRitual : Ritual(
    key = RitualKeys.KNOCKBACK,
    tags = RitualTags.NONE
) {
    override fun onCast(spell: GameObject) {
        val b = object : Behaviour(spell) {
            override fun onCollision(target: GameObject) {
                if (target.mass < 1000.0) {
                    val initX = target.x
                    TweenManager.tween(initX, initX + 16.0, 200.0, Easing.SMOOTH) { x, _ -> target.x = x }
                }
            }
        }
        spell.addBehaviour(b)
    }
}

object CeilingRitual : Ritual(
    key = RitualKeys.CEILING,
    tags = RitualTags.NONE,
    requiredTags = RitualTags.BOUNCING
) {
    override fun onActive() {
        resolveGame()?.stage?.ceiling = 48.0
    }
}

object RainRitual : Ritual(
    key = RitualKeys.RAIN,
    tags = RitualTags.SPLITTING,
    exclusiveTags = RitualTags.SPLITTING,
    recursive = false,
    rarity = Rarity.RARE
) {
    override fun onCast(spell: GameObject) {
        var split = false
        val rainBehaviour = object : Behaviour(spell) {
            override fun onFrame(dt: Double) {
                if (!split && gameObject.vy < 0.0) {
                    split = true
                    val game = resolveGame(gameObject) ?: return
                    val p1 = createSpell()
                    val p2 = createSpell()
                    p1.x = gameObject.x
                    p2.x = gameObject.x
                    p1.y = gameObject.y
                    p2.y = gameObject.y
                    p1.vx = gameObject.vx - 20.0
                    p2.vx = gameObject.vx + 20.0
                    p1.vy = gameObject.vy
                    p2.vy = gameObject.vy
                    p1.groupId = gameObject.groupId
                    p2.groupId = gameObject.groupId
                    game.onCast(p1, recursive = true)
                    game.onCast(p2, recursive = true)
                    game.spawn(p1)
                    game.spawn(p2)
                }
            }
        }
        spell.addBehaviour(rainBehaviour)
    }
}

object DrunkardRitual : Ritual(
    key = RitualKeys.DRUNKARD,
    tags = RitualTags.NONE
) {
    override fun onCast(spell: GameObject) {
        spell.vx += (randomInt(100) - 50).toDouble()
        spell.vy += (randomInt(100) - 50).toDouble()
        spell.getBehaviour<Damaging>()?.let { it.amount *= 2 }
    }
}

object SeerRitual : Ritual(
    key = RitualKeys.SEER,
    tags = RitualTags.NONE
) {
    override fun onCast(spell: GameObject) {
        spell.collisionMask = Tags.LIVING
    }
}

object TearstoneRitual : Ritual(
    key = RitualKeys.TEARSTONE,
    tags = RitualTags.NONE
) {
    override fun onCast(spell: GameObject) {
        val game = resolveGame(spell) ?: return
        if (game.player.hp < game.player.maxHp / 2.0) {
            spell.getBehaviour<Damaging>()?.let { it.amount *= 3 }
        }
    }
}

object ImpatienceRitual : Ritual(
    key = RitualKeys.IMPATIENCE,
    tags = RitualTags.NONE
) {
    override fun onActive() {
        resolveGame()?.ability?.let { it.cooldown /= 2.0 }
    }
}

object BleedRitual : Ritual(
    key = RitualKeys.BLEED,
    tags = RitualTags.CURSE
) {
    override fun onCast(spell: GameObject) {
        spell.spriteName = GameSprites.P_RED_SKULL
        spell.emitter?.apply {
            variants = listOf(
                listOf(GameSprites.P_RED_3, GameSprites.P_RED_2, GameSprites.P_RED_1),
                listOf(GameSprites.P_RED_4, GameSprites.P_RED_3, GameSprites.P_RED_2),
                listOf(GameSprites.P_RED_3, GameSprites.P_RED_2, GameSprites.P_RED_1)
            )
            frequency = 5.0
            angle = ParticleRange(DEG_180, 0.0)
            mass = ParticleRange(20.0, 50.0)
        }
        val inflict = object : Behaviour(spell) {
            override fun onCollision(target: GameObject) {
                target.addBehaviour(Bleeding(target))
            }
        }
        spell.addBehaviour(inflict)
    }
}

object AllegianceRitual : Ritual(
    key = RitualKeys.ALLEGIANCE,
    tags = RitualTags.NONE
) {
    override fun onResurrect() {
        val game = resolveGame() ?: return
        for (i in 0 until 3) {
            val unit = createSkeletonLord()
            unit.updateSpeed = 200.0
            game.spawn(unit, i * -15.0, 0.0)
        }
    }
}

object SalvageRitual : Ritual(
    key = RitualKeys.SALVAGE,
    tags = RitualTags.NONE
) {
    override fun onLevelEnd() {
        val game = resolveGame() ?: return
        val corpses = game.objects.filter { it.isTagged(Tags.CORPSE) }
        for (corpse in corpses) {
            val c = corpse.center()
            val emitter = Fx.bones(c.x, c.y).apply {
                variants = listOf(listOf(GameSprites.P_GREEN_SKULL))
                duration = ParticleRange(100.0, 1000.0)
            }
            emitter.burst(5)
            emitter.remove()
            game.despawn(corpse)
            game.addSouls(5)
        }
    }
}

object StudiousRitual : Ritual(
    key = RitualKeys.STUDIOUS,
    tags = RitualTags.NONE,
    rarity = Rarity.RARE
) {
    override fun onShopEnter() {
        for (item in ShopManager.items) {
            item.cost /= 2
        }
    }
}

object ElectrodynamicsRitual : Ritual(
    key = RitualKeys.ELECTRODYNAMICS,
    tags = RitualTags.NONE,
    rarity = Rarity.RARE
) {
    override fun onCast(spell: GameObject) {
        spell.addBehaviour(LightningStrike(spell))
    }
}

object ChillyRitual : Ritual(
    key = RitualKeys.CHILLY,
    tags = RitualTags.NONE
) {
    override fun onCast(spell: GameObject) {
        if (randomFloat() <= 0.1) {
            spell.emitter?.variants = listOf(listOf(GameSprites.P_ICE_1, GameSprites.P_ICE_2, GameSprites.P_ICE_3))
            spell.spriteName = GameSprites.P_SKULL
            spell.getBehaviour<Damaging>()?.amount = 0
            val freeze = object : Behaviour(spell) {
                override fun onCollision(target: GameObject) {
                    if (target.mass < 1000.0) {
                        target.addBehaviour(Frozen(target), 0)
                    }
                }
            }
            spell.addBehaviour(freeze)
        }
    }
}

object GiantsRitual : Ritual(
    key = RitualKeys.GIANTS,
    tags = RitualTags.NONE
) {
    override fun onResurrection(gameObject: GameObject) {
        if (randomFloat() < 0.2) {
            val game = resolveGame(gameObject) ?: return
            val x = gameObject.x
            val y = gameObject.y
            game.despawn(gameObject)
            game.spawn(createSkeletonLord(), x, y)
        }
    }
}

object AvariceRitual : Ritual(
    key = RitualKeys.AVARICE,
    tags = RitualTags.NONE
) {
    override fun onResurrection(gameObject: GameObject) {
        resolveGame(gameObject)?.addSouls(1)
    }
}

object HardenedRitual : Ritual(
    key = RitualKeys.HARDENED,
    tags = RitualTags.NONE
) {
    override fun onResurrection(gameObject: GameObject) {
        gameObject.maxHp += 1
        gameObject.hp = gameObject.maxHp
    }
}

/**
 * Returns the complete list of rituals available in the inter-wave shop.
 */
fun getAllShopRituals(): List<Ritual> = listOf(
    BouncingRitual,
    CeilingRitual,
    RainRitual,
    DoubleshotRitual,
    HunterRitual,
    WeightlessRitual,
    KnockbackRitual,
    DrunkardRitual,
    SeerRitual,
    TearstoneRitual,
    ImpatienceRitual,
    BleedRitual,
    SalvageRitual,
    StudiousRitual,
    ElectrodynamicsRitual,
    ChillyRitual,
    GiantsRitual,
    AvariceRitual,
    HardenedRitual,
    AllegianceRitual
)
