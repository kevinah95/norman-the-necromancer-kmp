package io.github.kevinah95.norman_the_necromancer.entities

import io.github.kevinah95.norman_the_necromancer.assets.GameSprites
import io.github.kevinah95.norman_the_necromancer.behaviours.*
import io.github.kevinah95.norman_the_necromancer.core.*
import io.github.kevinah95.norman_the_necromancer.fx.Fx
import io.github.kevinah95.norman_the_necromancer.fx.ParticleRange

fun createCorpse(): GameObject {
    return GameObject().apply {
        spriteName = GameSprites.SKULL
        spriteWidth = 8
        spriteHeight = 7
        mass = 100.0
        tags = Tags.CORPSE
    }
}

fun createPlayer(): GameObject {
    val player = GameObject().apply {
        x = 5.0
        spriteName = GameSprites.NORMAN_ARMS_DOWN
        spriteWidth = 16
        spriteHeight = 15
        tags = Tags.PLAYER or Tags.UNDEAD
        collisionMask = Tags.LIVING
        updateSpeed = 1000.0
        hp = 5
        maxHp = 5
        emitter = Fx.resurrect(bounds())
        onCollisionAction = { unit ->
            val game = gameSession as? Game
            game?.damage(this, unit.hp)
            game?.die(unit)
        }
        onDeathAction = {
            val game = gameSession as? Game
            val c = center()
            Fx.bones(c.x, c.y).burst(10)
            game?.state = GameState.LOSE
        }
    }
    return player
}

fun createSpell(): GameObject {
    val obj = GameObject().apply {
        spriteName = GameSprites.P_GREEN_SKULL
        spriteWidth = 4
        spriteHeight = 4
        tags = Tags.SPELL
        collisionMask = Tags.LIVING
        mass = 100.0
        emitter = Fx.trail()
        friction = 0.1
        despawnOnCollision = true
        despawnOnBounce = true
    }
    obj.addBehaviour(Damaging(obj))
    return obj
}

fun createBleedSpell(): GameObject {
    val spell = createSpell().apply {
        spriteName = GameSprites.P_RED_SKULL
        emitter?.apply {
            variants = listOf(
                listOf(GameSprites.P_RED_3, GameSprites.P_RED_2, GameSprites.P_RED_1),
                listOf(GameSprites.P_RED_4, GameSprites.P_RED_3, GameSprites.P_RED_2),
                listOf(GameSprites.P_RED_3, GameSprites.P_RED_2, GameSprites.P_RED_1)
            )
            frequency = 5.0
            angle = ParticleRange(DEG_180, 0.0)
            mass = ParticleRange(20.0, 50.0)
        }
    }
    val b = object : Behaviour(spell) {
        override fun onCollision(target: GameObject) {
            target.addBehaviour(Bleeding(target))
        }
    }
    spell.addBehaviour(b)
    return spell
}

fun createLightningSpell(): GameObject {
    val spell = createSpell().apply {
        spriteName = GameSprites.P_SKULL_YELLOW
        emitter?.apply {
            frequency = 0.8
            variants = listOf(
                listOf(GameSprites.P_LIGHTNING_1, GameSprites.P_LIGHTNING_2, GameSprites.P_LIGHTNING_3, GameSprites.P_LIGHTNING_4),
                listOf(GameSprites.P_LIGHTNING_1, GameSprites.P_LIGHTNING_2, GameSprites.P_LIGHTNING_3, GameSprites.P_LIGHTNING_5),
                listOf(GameSprites.P_LIGHTNING_2, GameSprites.P_LIGHTNING_3, GameSprites.P_LIGHTNING_6),
                listOf(GameSprites.P_LIGHTNING_4, GameSprites.P_LIGHTNING_5, GameSprites.P_LIGHTNING_6),
                listOf(GameSprites.P_PURPLE_5)
            )
        }
    }
    return spell
}

fun createSkeleton(): GameObject {
    val unit = GameObject().apply {
        spriteName = GameSprites.SKELETON
        spriteWidth = 13
        spriteHeight = 15
        tags = Tags.UNDEAD or Tags.MOBILE
        collisionMask = Tags.LIVING
        hp = 1
        maxHp = 1
        updateSpeed = 1000.0
    }
    unit.addBehaviour(March(unit, 16.0))
    unit.addBehaviour(Attack(unit))
    return unit
}

fun createSkeletonLord(): GameObject {
    val unit = createSkeleton().apply {
        spriteName = GameSprites.BIG_SKELETON
        spriteWidth = 11
        spriteHeight = 20
        hp = 3
        maxHp = 3
        updateSpeed = 1500.0
    }
    return unit
}

/**
 * Base factory for hostile mobile units that march left towards Norman.
 * Centralizes standard physics, collision tags, and march behaviour.
 */
fun createEnemy(
    spriteName: String,
    spriteWidth: Int,
    spriteHeight: Int,
    hp: Int = 1,
    souls: Int = 5,
    updateSpeed: Double = 600.0,
    corpseChance: Double = 0.75,
    marchStep: Double = -16.0
): GameObject {
    val unit = GameObject().apply {
        this.spriteName = spriteName
        this.spriteWidth = spriteWidth
        this.spriteHeight = spriteHeight
        this.friction = 0.8
        this.mass = 75.0
        this.x = 400.0
        this.tags = Tags.LIVING or Tags.MOBILE
        this.hp = hp
        this.maxHp = hp
        this.updateSpeed = updateSpeed
        this.corpseChance = corpseChance
        this.souls = souls
    }
    unit.addBehaviour(March(unit, marchStep))
    return unit
}

fun createVillager(): GameObject {
    val variants = listOf(GameSprites.VILLAGER_1, GameSprites.VILLAGER_2, GameSprites.VILLAGER_3, GameSprites.VILLAGER_4)
    val selectedSprite = variants.randomElement()
    val width = when (selectedSprite) {
        GameSprites.VILLAGER_1 -> 14
        GameSprites.VILLAGER_2 -> 12
        GameSprites.VILLAGER_3 -> 13
        else -> 14
    }
    return createEnemy(
        spriteName = selectedSprite,
        spriteWidth = width,
        spriteHeight = 15,
        hp = 1,
        souls = 5,
        updateSpeed = 600.0,
        corpseChance = 0.75
    )
}

fun createBandit(): GameObject {
    return createVillager().apply {
        hp = 2
        maxHp = 2
    }
}

fun createArcher(): GameObject = createEnemy(
    spriteName = GameSprites.ARCHER,
    spriteWidth = 13,
    spriteHeight = 15,
    hp = 2,
    updateSpeed = 300.0
)

fun createMonk(): GameObject {
    val unit = createEnemy(
        spriteName = GameSprites.MONK,
        spriteWidth = 10,
        spriteHeight = 15,
        hp = 3,
        souls = 10,
        updateSpeed = 600.0
    )
    val heal = object : Behaviour(unit) {
        init { turns = 5 }
        override fun onUpdate(): Boolean {
            val game = gameObject.gameSession as? Game ?: return false
            for (obj in game.objects) {
                if (obj.isTagged(Tags.LIVING)) {
                    game.damage(obj, -1, gameObject)
                }
            }
            Fx.holy(gameObject.bounds()).burst(10).remove()
            return false
        }
    }
    unit.addBehaviour(heal)
    return unit
}

fun createChampion(): GameObject = createEnemy(
    spriteName = GameSprites.CHAMPION,
    spriteWidth = 22,
    spriteHeight = 20,
    hp = 10,
    souls = 25,
    updateSpeed = 1000.0
)

fun createShellKnight(): GameObject {
    val unit = createEnemy(
        spriteName = GameSprites.SHELL_KNIGHT_UP,
        spriteWidth = 18,
        spriteHeight = 17,
        hp = 5,
        souls = 15,
        updateSpeed = 1000.0
    )

    var shelled = false
    var timer = 0
    val shell = object : Behaviour(unit) {
        override fun onUpdate(): Boolean {
            shelled = (timer++ % 4) > 1
            gameObject.spriteName = if (shelled) GameSprites.SHELL_KNIGHT_DOWN else GameSprites.SHELL_KNIGHT_UP
            gameObject.spriteWidth = if (shelled) 20 else 18
            gameObject.spriteHeight = if (shelled) 11 else 17
            spriteName = if (shelled) GameSprites.STATUS_SHIELDED else null
            return false
        }

        override fun onDamage(damage: Damage) {
            if (shelled) {
                damage.amount = minOf(0, damage.amount)
            }
        }
    }
    unit.addBehaviour(shell)
    return unit
}

fun createPiper(): GameObject {
    val unit = createEnemy(
        spriteName = GameSprites.PIPER,
        spriteWidth = 12,
        spriteHeight = 14,
        hp = 15,
        souls = 100,
        updateSpeed = 500.0
    )
    unit.addBehaviour(Summon(unit, ::createRat, 2000.0))
    return unit
}

fun createRat(): GameObject = createEnemy(
    spriteName = GameSprites.RAT,
    spriteWidth = 20,
    spriteHeight = 6,
    hp = 1,
    souls = 5,
    updateSpeed = 200.0,
    corpseChance = 0.0
)

fun createRageKnight(): GameObject {
    val unit = createEnemy(
        spriteName = GameSprites.RAGE_KNIGHT,
        spriteWidth = 14,
        spriteHeight = 15,
        hp = 5,
        souls = 20,
        updateSpeed = 500.0
    )

    val march = unit.getBehaviour<March>()
    val step = march?.step ?: -16.0
    val enraged = Enraged(unit, Tags.SPELL)
    var angry = false

    val raging = object : Behaviour(unit) {
        init { turns = 5 }
        override fun onUpdate(): Boolean {
            angry = !angry
            if (angry) {
                gameObject.addBehaviour(enraged)
                gameObject.spriteName = GameSprites.RAGE_KNIGHT_ENRAGED
                gameObject.spriteWidth = 15
                gameObject.spriteHeight = 20
                march?.step = 0.0
            } else {
                gameObject.removeBehaviour(enraged)
                gameObject.spriteName = GameSprites.RAGE_KNIGHT
                gameObject.spriteWidth = 14
                gameObject.spriteHeight = 15
                march?.step = step
            }
            return false
        }
    }
    unit.addBehaviour(raging)
    return unit
}

fun createRoyalGuardOrb(): GameObject {
    val orb = GameObject().apply {
        spriteName = GameSprites.YELLOW_ORB
        spriteWidth = 5
        spriteHeight = 5
        tags = Tags.SPELL
        collisionMask = Tags.MOBILE or Tags.PLAYER
        hp = 1
        despawnOnBounce = true
        despawnOnCollision = true
        friction = 0.9
        emitter = Fx.royalty()
    }
    orb.addBehaviour(Damaging(orb))
    orb.addBehaviour(DespawnTimer(orb, 3000.0))
    return orb
}

fun createRoyalGuard(): GameObject {
    val unit = createEnemy(
        spriteName = GameSprites.ROYAL_GUARD,
        spriteWidth = 16,
        spriteHeight = 17,
        hp = 4,
        souls = 10
    )
    val march = unit.getBehaviour<March>()
    var shielded = false

    val shield = object : Behaviour(unit) {
        init { turns = 3 }
        override fun onUpdate(): Boolean {
            shielded = !shielded
            march?.step = if (shielded) 0.0 else -16.0
            gameObject.spriteName = if (shielded) GameSprites.ROYAL_GUARD_SHIELDED else GameSprites.ROYAL_GUARD
            gameObject.spriteWidth = if (shielded) 15 else 16
            return false
        }

        override fun onDamage(damage: Damage) {
            val dealer = damage.dealer ?: return
            if (!shielded || !dealer.isTagged(Tags.SPELL)) return
            if (dealer.vx > 0) {
                damage.amount = 0
                val orb = createRoyalGuardOrb()
                dealer.vx *= -1.0
                dealer.vy *= -0.25
                orb.vx = dealer.vx
                orb.vy = dealer.vy
                orb.mass = dealer.mass
                gameObject.gameSession?.spawn(orb, dealer.x - orb.spriteWidth - 1, dealer.y)
            }
        }
    }

    unit.behaviours.reverse() // Shield added first so it comes up first
    unit.addBehaviour(shield, 0)
    return unit
}

fun createWizard(): GameObject {
    val unit = createEnemy(
        spriteName = GameSprites.WIZARD,
        spriteWidth = 14,
        spriteHeight = 17,
        hp = 15,
        souls = 10,
        updateSpeed = 500.0
    )
    unit.addBehaviour(Summon(unit, ::createPortal, 3000.0))
    return unit
}

fun createPortal(): GameObject {
    val unit = GameObject().apply {
        spriteName = GameSprites.PORTAL
        spriteWidth = 9
        spriteHeight = 15
        tags = Tags.LIVING
        hp = 3
        maxHp = 3
        souls = 10
    }
    unit.addBehaviour(DespawnTimer(unit, 30000.0))
    unit.addBehaviour(
        Summon(
            unit,
            { listOf(::createVillager, ::createBandit, ::createArcher).randomElement().invoke() },
            3000.0
        )
    )
    unit.emitter = Fx.portal(unit.bounds())
    return unit
}

fun createTheKing(): GameObject {
    val unit = createEnemy(
        spriteName = GameSprites.THE_KING,
        spriteWidth = 29,
        spriteHeight = 31,
        hp = 100,
        souls = 5,
        updateSpeed = 5000.0
    ).apply {
        behaviours.clear()
        mass = 1000.0
        emitter = Fx.royalty().apply {
            frequency = 0.2
            angle = ParticleRange(DEG_90, 0.5)
            w = 29.0
            h = 31.0
        }
    }

    var phase = 1
    val marching = March(unit, -32.0)
    val summons = Summon(unit, ::createRoyalGuard, 2000.0)
    val enraged = Enraged(unit, Tags.SPELL)
    val invulnerable = Invulnerable(unit)

    val boss = object : Behaviour(unit) {
        override fun onDamage(damage: Damage) {
            val willDie = (gameObject.hp - damage.amount) <= 0
            if (phase == 1 && willDie) {
                phase = 2
                gameObject.addBehaviour(summons)
                gameObject.addBehaviour(enraged)
                gameObject.addBehaviour(invulnerable)
                marching.step *= -1.0
            } else if (phase == 3 && willDie) {
                phase = 4
                io.github.kevinah95.norman_the_necromancer.audio.GameAudio.onKingPhase4()
                gameObject.hp = gameObject.maxHp
                gameObject.spriteName = GameSprites.THE_KING_ON_FOOT
                gameObject.spriteWidth = 22
                gameObject.spriteHeight = 22
                gameObject.updateSpeed = 1000.0
                gameObject.updateClock = 1000.0
                marching.step /= 2.0

                var rainTimer = 0.0
                val rainCorpse = object : Behaviour(gameObject) {
                    override fun onFrame(dt: Double) {
                        rainTimer += dt
                        if (rainTimer > 300.0) {
                            rainTimer = 0.0
                            val game = gameObject.gameSession as? Game ?: return
                            game.spawn(createCorpse(), randomInt(game.stage.width.toInt()).toDouble(), game.stage.ceiling)
                        }
                    }
                }
                gameObject.addBehaviour(rainCorpse)
            }
        }
    }

    summons.onSummonAction = {
        if (summons.summonCounter >= 5) {
            phase = 3
            unit.removeBehaviour(enraged)
            unit.removeBehaviour(invulnerable)
            unit.removeBehaviour(summons)
            marching.step *= -1.0
        }
    }

    unit.addBehaviour(marching)
    unit.addBehaviour(boss)
    return unit
}
