package io.github.kevinah95.norman_the_necromancer.rituals

import io.github.kevinah95.norman_the_necromancer.assets.GameSprites
import io.github.kevinah95.norman_the_necromancer.behaviours.*
import io.github.kevinah95.norman_the_necromancer.core.*
import io.github.kevinah95.norman_the_necromancer.entities.createSkeletonLord
import io.github.kevinah95.norman_the_necromancer.entities.createSpell
import io.github.kevinah95.norman_the_necromancer.fx.Fx
import io.github.kevinah95.norman_the_necromancer.fx.ParticleRange
import io.github.kevinah95.norman_the_necromancer.shop.ShopManager


// Ritual tags bitmasks
const val TAG_NONE: Int = 0
const val TAG_BOUNCING: Int = 1 shl 0
const val TAG_SPLITTING: Int = 1 shl 1
const val TAG_EXPLOSIVE: Int = 1 shl 2
const val TAG_HOMING: Int = 1 shl 3
const val TAG_WARDSTONES: Int = 1 shl 4
const val TAG_CASTING_RATE: Int = 1 shl 5
const val TAG_CURSE: Int = 1 shl 6

val StreakRitual = object : Ritual("Streak", "", TAG_NONE) {
    override fun onCast(spell: GameObject) {
        spell.addBehaviour(HitStreak(spell))
    }
}

val BouncingRitual = object : Ritual(
    "Bouncing",
    "Spells bounce",
    TAG_BOUNCING
) {
    override fun onCast(spell: GameObject) {
        spell.addBehaviour(DespawnTimer(spell, 3000.0))
        spell.despawnOnBounce = false
        spell.bounce = 0.5
    }
}

val DoubleshotRitual = object : Ritual(
    "Doubleshot",
    "Cast 2 spells",
    TAG_SPLITTING,
    exclusiveTags = TAG_SPLITTING,
    rarity = Rarity.RARE
) {
    override fun onActive() {
        val game = game ?: return
        game.spell.shotsPerRound = 2
    }
}

val HunterRitual = object : Ritual(
    "Hunter",
    "Spells seek targets",
    TAG_HOMING,
    rarity = Rarity.RARE
) {
    override fun onCast(spell: GameObject) {
        spell.addBehaviour(Seeking(spell))
    }
}

val WeightlessRitual = object : Ritual(
    "Weightless",
    "Spells are not affected by gravity",
    TAG_NONE
) {
    override fun onCast(spell: GameObject) {
        spell.mass = 0.0
        spell.friction = 0.0
        spell.bounce = 1.0
    }
}

val KnockbackRitual = object : Ritual(
    "Knockback",
    "Spells knock backwards",
    TAG_NONE
) {
    override fun onCast(spell: GameObject) {
        val b = object : Behaviour(spell) {
            override fun onCollision(target: GameObject) {
                if (target.mass < 1000.0) {
                    val initX = target.x
                    TweenManager.tween(initX, initX + 16.0, 200.0, korlibs.math.interpolation.Easing.SMOOTH) { x, _ -> target.x = x }
                }
            }
        }
        spell.addBehaviour(b)
    }
}

val CeilingRitual = object : Ritual(
    "Ceiling",
    "Adds a ceiling",
    TAG_NONE,
    requiredTags = TAG_BOUNCING
) {
    override fun onActive() {
        val game = game ?: return
        game.stage.ceiling = 48.0
    }
}

val RainRitual = object : Ritual(
    "Rain",
    "Spells split when they drop",
    TAG_SPLITTING,
    exclusiveTags = TAG_SPLITTING,
    recursive = false,
    rarity = Rarity.RARE
) {
    override fun onCast(spell: GameObject) {
        var split = false
        val rainBehaviour = object : Behaviour(spell) {
            override fun onFrame(dt: Double) {
                if (!split && gameObject.vy < 0.0) {
                    split = true
                    val game = gameObject.gameSession as? Game ?: return
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

val DrunkardRitual = object : Ritual(
    "Drunkard",
    "2x damage, wobbly aim",
    TAG_NONE
) {
    override fun onCast(spell: GameObject) {
        spell.vx += (randomInt(100) - 50).toDouble()
        spell.vy += (randomInt(100) - 50).toDouble()
        spell.getBehaviour<Damaging>()?.let { it.amount *= 2 }
    }
}

val SeerRitual = object : Ritual(
    "Seer",
    "Spells pass through the dead",
    TAG_NONE
) {
    override fun onCast(spell: GameObject) {
        spell.collisionMask = Tags.LIVING
    }
}

val TearstoneRitual = object : Ritual(
    "Tearstone",
    "3x damage when < half HP",
    TAG_NONE
) {
    override fun onCast(spell: GameObject) {
        val game = gameObjectSession(spell) ?: return
        if (game.player.hp < game.player.maxHp / 2.0) {
            spell.getBehaviour<Damaging>()?.let { it.amount *= 3 }
        }
    }
}

val ImpatienceRitual = object : Ritual(
    "Impatience",
    "Resurrection recharges 2x faster",
    TAG_NONE
) {
    override fun onActive() {
        val game = game ?: return
        game.ability.cooldown /= 2.0
    }
}

val BleedRitual = object : Ritual(
    "Bleed",
    "Inflicts bleed on hits",
    TAG_CURSE
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

val AllegianceRitual = object : Ritual(
    "Allegiance",
    "Summon your honour guard after resurrections",
    TAG_NONE
) {
    override fun onResurrect() {
        val game = game ?: return
        for (i in 0 until 3) {
            val unit = createSkeletonLord()
            unit.updateSpeed = 200.0
            game.spawn(unit, i * -15.0, 0.0)
        }
    }
}

val SalvageRitual = object : Ritual(
    "Salvage",
    "Corpses become souls at end of level",
    TAG_NONE
) {
    override fun onLevelEnd() {
        val game = game ?: return
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

val StudiousRitual = object : Ritual(
    "Studious",
    "Rituals are 50% cheaper",
    TAG_NONE,
    rarity = Rarity.RARE
) {
    override fun onShopEnter() {
        for (item in ShopManager.items) {
            item.cost = item.cost / 2
        }
    }
}

val ElectrodynamicsRitual = object : Ritual(
    "Electrodynamics",
    "Lightning strikes after hits",
    TAG_NONE,
    rarity = Rarity.RARE
) {
    override fun onCast(spell: GameObject) {
        spell.addBehaviour(LightningStrike(spell))
    }
}

val ChillyRitual = object : Ritual(
    "Chilly",
    "10% chance to freeze enemies",
    TAG_NONE
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

val GiantsRitual = object : Ritual(
    "Giants",
    "20% chance to resurrect giant skeletons",
    TAG_NONE
) {
    override fun onResurrection(gameObject: GameObject) {
        if (randomFloat() < 0.2) {
            val game = (game ?: gameObjectSession(gameObject)) ?: return
            val x = gameObject.x
            val y = gameObject.y
            game.despawn(gameObject)
            game.spawn(createSkeletonLord(), x, y)
        }
    }
}

val AvariceRitual = object : Ritual(
    "Avarice",
    "+1 soul for each corpse you resurrect",
    TAG_NONE
) {
    override fun onResurrection(gameObject: GameObject) {
        (game ?: gameObjectSession(gameObject))?.addSouls(1)
    }
}

val HardenedRitual = object : Ritual(
    "Hardened",
    "Undead have +1 HP",
    TAG_NONE
) {
    override fun onResurrection(gameObject: GameObject) {
        gameObject.maxHp += 1
        gameObject.hp = gameObject.maxHp
    }
}

private fun gameObjectSession(obj: GameObject): Game? = obj.gameSession as? Game

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
