package io.github.kevinah95.norman_the_necromancer.entities

import io.github.kevinah95.norman_the_necromancer.behaviours.*
import io.github.kevinah95.norman_the_necromancer.core.*
import io.github.kevinah95.norman_the_necromancer.fx.Fx
import io.github.kevinah95.norman_the_necromancer.fx.ParticleRange

fun createCorpse(): GameObject {
    return GameObject().apply {
        spriteName = "skull"
        spriteWidth = 8
        spriteHeight = 7
        mass = 100.0
        tags = Tags.CORPSE
    }
}

fun createPlayer(): GameObject {
    val player = GameObject().apply {
        x = 5.0
        spriteName = "norman_arms_down"
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
        spriteName = "p_green_skull"
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
        spriteName = "p_red_skull"
        emitter?.apply {
            variants = listOf(
                listOf("p_red_3", "p_red_2", "p_red_1"),
                listOf("p_red_4", "p_red_3", "p_red_2"),
                listOf("p_red_3", "p_red_2", "p_red_1")
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
        spriteName = "p_skull_yellow"
        emitter?.apply {
            frequency = 0.8
            variants = listOf(
                listOf("p_lightning_1", "p_lightning_2", "p_lightning_3", "p_lightning_4"),
                listOf("p_lightning_1", "p_lightning_2", "p_lightning_3", "p_lightning_5"),
                listOf("p_lightning_2", "p_lightning_3", "p_lightning_6"),
                listOf("p_lightning_4", "p_lightning_5", "p_lightning_6"),
                listOf("p_purple_5")
            )
        }
    }
    return spell
}

fun createSkeleton(): GameObject {
    val unit = GameObject().apply {
        spriteName = "skeleton"
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
        spriteName = "big_skeleton"
        spriteWidth = 11
        spriteHeight = 20
        hp = 3
        maxHp = 3
        updateSpeed = 1500.0
    }
    return unit
}

fun createVillager(): GameObject {
    val variants = listOf("villager_1", "villager_2", "villager_3", "villager_4")
    val selectedSprite = variants.randomElement()
    val unit = GameObject().apply {
        spriteName = selectedSprite
        spriteWidth = when (selectedSprite) {
            "villager_1" -> 14
            "villager_2" -> 12
            "villager_3" -> 13
            else -> 14
        }
        spriteHeight = 15
        friction = 0.8
        mass = 75.0
        x = 400.0
        tags = Tags.LIVING or Tags.MOBILE
        hp = 1
        maxHp = 1
        updateSpeed = 600.0
        corpseChance = 0.75
        souls = 5
    }
    unit.addBehaviour(March(unit, -16.0))
    return unit
}

fun createBandit(): GameObject {
    val unit = createVillager().apply {
        hp = 2
        maxHp = 2
    }
    return unit
}

fun createArcher(): GameObject {
    val unit = createVillager().apply {
        spriteName = "archer"
        spriteWidth = 13
        spriteHeight = 15
        updateSpeed = 300.0
        hp = 2
        maxHp = 2
    }
    return unit
}

fun createMonk(): GameObject {
    val unit = createVillager().apply {
        spriteName = "monk"
        spriteWidth = 10
        spriteHeight = 15
        updateSpeed = 600.0
        hp = 3
        maxHp = 3
        souls = 10
    }
    val heal = object : Behaviour(unit) {
        init { turns = 5 }
        override fun onUpdate(): Boolean {
            val game = gameObject.gameSession as? Game ?: return false
            for (obj in game.objects) {
                if (obj.isTagged(Tags.LIVING)) {
                    game.damage(obj, -1, gameObject)
                }
            }
            Fx.cloud(
                gameObject.bounds(),
                listOf(
                    listOf("p_star_1", "p_star_2", "p_star_3"),
                    listOf("p_star_2", "p_star_3", "p_star_4"),
                    listOf("p_star_1", "p_star_3")
                )
            ).burst(10).remove()
            return false
        }
    }
    unit.addBehaviour(heal)
    return unit
}

fun createChampion(): GameObject {
    val unit = createVillager().apply {
        spriteName = "champion"
        spriteWidth = 22
        spriteHeight = 20
        updateSpeed = 1000.0
        hp = 10
        maxHp = 10
        souls = 25
    }
    return unit
}

fun createShellKnight(): GameObject {
    val unit = createVillager().apply {
        spriteName = "shell_knight_up"
        spriteWidth = 18
        spriteHeight = 17
        updateSpeed = 1000.0
        hp = 5
        maxHp = 5
        souls = 15
    }

    var shelled = false
    var timer = 0
    val shell = object : Behaviour(unit) {
        override fun onUpdate(): Boolean {
            shelled = (timer++ % 4) > 1
            gameObject.spriteName = if (shelled) "shell_knight_down" else "shell_knight_up"
            gameObject.spriteWidth = if (shelled) 20 else 18
            gameObject.spriteHeight = if (shelled) 11 else 17
            spriteName = if (shelled) "status_shielded" else null
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
    val unit = createVillager().apply {
        spriteName = "piper"
        spriteWidth = 12
        spriteHeight = 14
        updateSpeed = 500.0
        hp = 15
        maxHp = 15
        souls = 100
    }
    unit.addBehaviour(Summon(unit, ::createRat, 2000.0))
    return unit
}

fun createRat(): GameObject {
    val unit = createVillager().apply {
        spriteName = "rat"
        spriteWidth = 20
        spriteHeight = 6
        updateSpeed = 200.0
        souls = 5
        corpseChance = 0.0
    }
    return unit
}

fun createRageKnight(): GameObject {
    val unit = createVillager().apply {
        spriteName = "rage_knight"
        spriteWidth = 14
        spriteHeight = 15
        updateSpeed = 500.0
        hp = 5
        maxHp = 5
        souls = 20
    }

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
                gameObject.spriteName = "rage_knight_enraged"
                gameObject.spriteWidth = 15
                gameObject.spriteHeight = 20
                march?.step = 0.0
            } else {
                gameObject.removeBehaviour(enraged)
                gameObject.spriteName = "rage_knight"
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
        spriteName = "yellow_orb"
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
    val unit = createVillager().apply {
        spriteName = "royal_guard"
        spriteWidth = 16
        spriteHeight = 17
        hp = 4
        maxHp = 4
        souls = 10
    }
    val march = unit.getBehaviour<March>()
    var shielded = false

    val shield = object : Behaviour(unit) {
        init { turns = 3 }
        override fun onUpdate(): Boolean {
            shielded = !shielded
            march?.step = if (shielded) 0.0 else -16.0
            gameObject.spriteName = if (shielded) "royal_guard_shielded" else "royal_guard"
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
    val unit = createVillager().apply {
        spriteName = "wizard"
        spriteWidth = 14
        spriteHeight = 17
        hp = 15
        maxHp = 15
        souls = 10
    }
    unit.addBehaviour(Summon(unit, ::createPortal, 3000.0))
    return unit
}

fun createPortal(): GameObject {
    val unit = GameObject().apply {
        spriteName = "portal"
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
    unit.emitter = Fx.cloud(
        unit.bounds(),
        listOf(
            listOf("p_blue_1", "p_blue_2", "p_blue_3"),
            listOf("p_blue_2", "p_blue_3"),
            listOf("p_blue_3")
        )
    ).apply { frequency = 0.2 }
    return unit
}

fun createTheKing(): GameObject {
    val unit = createVillager().apply {
        spriteName = "the_king"
        spriteWidth = 29
        spriteHeight = 31
        updateSpeed = 5000.0
        hp = 100
        maxHp = 100
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
                gameObject.spriteName = "the_king_on_foot"
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
