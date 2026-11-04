package io.github.kevinah95.norman_the_necromancer.entities

import io.github.kevinah95.norman_the_necromancer.assets.GameSprites
import io.github.kevinah95.norman_the_necromancer.behaviours.*
import io.github.kevinah95.norman_the_necromancer.core.*
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EntitiesAndBehavioursTest {

    private lateinit var game: Game
    private lateinit var player: GameObject

    @BeforeTest
    fun setUp() {
        gameRandom = Random(seed = 42)
        TweenManager.reset()
        HitStreak.counters.clear()
        val p = createPlayer()
        player = p
        game = Game().apply {
            this.player = p
            state = GameState.PLAYING
            spawn(p, 5.0, 0.0)
        }
    }

    @AfterTest
    fun tearDown() {
        gameRandom = Random.Default
        TweenManager.reset()
        HitStreak.counters.clear()
    }

    @Test
    fun testAllEnemyFactoriesAndVillagerVariants() {
        // Spawn multiple villagers with deterministic random so all variants are exercised
        val spritesSeen = mutableSetOf<String>()
        for (i in 0 until 30) {
            val v = createVillager()
            spritesSeen.add(v.spriteName)
            assertEquals(1, v.hp)
        }
        assertEquals(4, spritesSeen.size, "All 4 villager sprite variants should be generated")

        val bandit = createBandit()
        assertEquals(2, bandit.hp)
        assertEquals(2, bandit.maxHp)

        val archer = createArcher()
        assertEquals(GameSprites.ARCHER, archer.spriteName)
        assertEquals(2, archer.hp)
        assertEquals(300.0, archer.updateSpeed)

        val champion = createChampion()
        assertEquals(GameSprites.CHAMPION, champion.spriteName)
        assertEquals(10, champion.hp)
        assertEquals(25, champion.souls)

        val rat = createRat()
        assertEquals(GameSprites.RAT, rat.spriteName)
        assertEquals(0.0, rat.corpseChance)

        val skeletonLord = createSkeletonLord()
        assertEquals(GameSprites.BIG_SKELETON, skeletonLord.spriteName)
        assertEquals(3, skeletonLord.hp)
    }

    @Test
    fun testPlayerCollisionWithEnemyDealsDamageAndKillsUnit() {
        val villager = createVillager().apply { hp = 2 }
        game.spawn(villager, 5.0, 0.0)

        // Update physics so player and villager collide
        game.update(16.0)
        assertEquals(3, player.hp)
        assertFalse(villager in game.objects)

        // Skeleton Attack behaviour on collision with living enemy
        val skeleton = createSkeleton()
        val targetEnemy = createVillager()
        game.spawn(skeleton, 50.0, 0.0)
        game.spawn(targetEnemy, 50.0, 0.0)
        game.update(16.0)
        assertFalse(skeleton in game.objects)
        assertFalse(targetEnemy in game.objects)

        // Default createEnemy parameters
        val defaultEnemy = createEnemy(GameSprites.VILLAGER_1, 14, 15)
        assertEquals(1, defaultEnemy.hp)
    }

    @Test
    fun testBleedSpellAndBleedingBehaviour() {
        val bleedSpell = createBleedSpell()
        assertEquals(GameSprites.P_RED_SKULL, bleedSpell.spriteName)

        val champion = createChampion()
        game.spawn(champion, 100.0, 0.0)
        game.spawn(bleedSpell, 100.0, 0.0)

        // Collide spell with champion (1 initial damage + applies Bleeding)
        game.update(16.0)
        assertEquals(9, champion.hp)
        val bleeding = champion.getBehaviour<Bleeding>()
        assertNotNull(bleeding)
        assertEquals(GameSprites.STATUS_BLEEDING, bleeding.spriteName)

        //Bleeding triggers every 3 turns of champion's onUpdate
        champion.onUpdate()
        champion.onUpdate()
        assertEquals(9, champion.hp)
        champion.onUpdate()
        assertEquals(8, champion.hp)
    }

    @Test
    fun testMonkHealsLivingAlliesEveryFiveTurns() {
        val monk = createMonk()
        val champion = createChampion().apply { hp = 5 } // maxHp = 10
        game.spawn(monk, 200.0, 0.0)
        game.spawn(champion, 150.0, 0.0)

        for (turn in 1..4) {
            monk.onUpdate()
        }
        assertEquals(5, champion.hp)

        // 5th turn triggers AoE heal (-1 damage)
        monk.onUpdate()
        assertEquals(6, champion.hp)
    }

    @Test
    fun testShellKnightRetreatsIntoShellAndBlocksDamage() {
        val knight = createShellKnight()
        game.spawn(knight, 200.0, 0.0)
        assertEquals(GameSprites.SHELL_KNIGHT_UP, knight.spriteName)

        // timer=0 -> (0 % 4 > 1) = false
        knight.onUpdate()
        assertEquals(GameSprites.SHELL_KNIGHT_UP, knight.spriteName)
        game.damage(knight, 1)
        assertEquals(4, knight.hp)

        // timer=1 -> (1 % 4 > 1) = false
        knight.onUpdate()
        assertEquals(GameSprites.SHELL_KNIGHT_UP, knight.spriteName)

        // timer=2 -> (2 % 4 > 1) = true -> Shelled!
        knight.onUpdate()
        assertEquals(GameSprites.SHELL_KNIGHT_DOWN, knight.spriteName)
        game.damage(knight, 2)
        assertEquals(4, knight.hp, "Shell Knight should take 0 damage while shelled")

        // Healing (negative damage) should still work while shelled
        game.damage(knight, -1)
        assertEquals(5, knight.hp)
    }

    @Test
    fun testRageKnightTogglesEnragedAndHealsFromSpellDamage() {
        val rageKnight = createRageKnight()
        game.spawn(rageKnight, 200.0, 0.0)
        rageKnight.hp = 3 // maxHp = 5

        // Trigger 5 turns to enter enraged mode
        for (i in 1..5) {
            rageKnight.onUpdate()
        }
        assertEquals(GameSprites.RAGE_KNIGHT_ENRAGED, rageKnight.spriteName)
        assertNotNull(rageKnight.getBehaviour<Enraged>())
        assertEquals(0.0, rageKnight.getBehaviour<March>()?.step)

        // Hit with a spell -> Enraged absorbs damage and heals instead!
        val spell = createSpell()
        game.damage(rageKnight, 1, spell)
        assertEquals(4, rageKnight.hp)

        // Trigger 5 more turns to exit enraged mode
        for (i in 1..5) {
            rageKnight.onUpdate()
        }
        assertEquals(GameSprites.RAGE_KNIGHT, rageKnight.spriteName)
        assertNull(rageKnight.getBehaviour<Enraged>())
    }

    @Test
    fun testRoyalGuardDeflectsSpellsWhenShielded() {
        val guard = createRoyalGuard()
        game.spawn(guard, 200.0, 0.0)
        assertEquals(GameSprites.ROYAL_GUARD, guard.spriteName)

        // 3 turns to raise shield
        for (i in 1..3) {
            guard.onUpdate()
        }
        assertEquals(GameSprites.ROYAL_GUARD_SHIELDED, guard.spriteName)

        val spell = createSpell().apply {
            x = 195.0
            y = 5.0
            vx = 180.0
            vy = 20.0
        }
        game.spawn(spell, 195.0, 5.0)

        // Damage guard with incoming spell moving right (vx > 0)
        game.damage(guard, 1, spell)
        assertEquals(4, guard.hp, "Shielded Royal Guard should parry incoming spell")
        assertTrue(spell.vx < 0.0, "Spell velocity should be reversed")

        val deflectedOrb = game.objects.firstOrNull { it.spriteName == GameSprites.YELLOW_ORB }
        assertNotNull(deflectedOrb, "Royal Guard should spawn a yellow retaliation orb")

        // Advance 3000ms so DespawnTimer despawns the orb
        game.update(3000.0)
        assertFalse(deflectedOrb in game.objects)
    }

    @Test
    fun testPiperWizardAndPortalSummoning() {
        val piper = createPiper()
        game.spawn(piper, 250.0, 0.0)
        game.update(2001.0)
        assertTrue(game.objects.any { it.spriteName == GameSprites.RAT }, "Piper should summon a Rat")

        val wizard = createWizard()
        game.spawn(wizard, 300.0, 0.0)
        game.update(3001.0)
        val portal = game.objects.firstOrNull { it.spriteName == GameSprites.PORTAL }
        assertNotNull(portal, "Wizard should summon a Portal")

        // Portal summons reinforcement after 3000ms
        val countBefore = game.objects.size
        game.update(3001.0)
        assertTrue(game.objects.size > countBefore, "Portal should summon an enemy unit")
    }

    @Test
    fun testTheKingFourPhaseBossStateMachine() {
        val king = createTheKing()
        game.spawn(king, 300.0, 0.0)
        assertEquals(100, king.hp)
        assertEquals(GameSprites.THE_KING, king.spriteName)

        // Phase 1 -> Deal lethal damage to trigger Phase 2 (retreats invulnerable & summons guards)
        king.hp = 1
        val spell = createSpell()
        game.damage(king, 5, spell)
        assertTrue(king in game.objects, "King must survive Phase 1 lethal hit and enter Phase 2")
        assertNotNull(king.getBehaviour<Invulnerable>())
        assertNotNull(king.getBehaviour<Enraged>())
        assertNotNull(king.getBehaviour<Summon>())
        assertEquals(32.0, king.getBehaviour<March>()?.step)

        // Phase 2 -> Hit King while Invulnerable is active -> takes 0 damage
        val hpInPhase2 = king.hp
        game.damage(king, 25)
        assertEquals(hpInPhase2, king.hp, "King must not take damage while Invulnerable in Phase 2")

        // Phase 2 -> Summon 5 Royal Guards (1 every 2000ms) to transition to Phase 3
        for (i in 1..5) {
            king.onFrame(2001.0)
        }
        val guardsCount = game.objects.count { it.spriteName == GameSprites.ROYAL_GUARD }
        assertEquals(5, guardsCount, "King should have summoned 5 Royal Guards")
        assertNull(king.getBehaviour<Invulnerable>(), "King should become vulnerable in Phase 3")
        assertNull(king.getBehaviour<Enraged>())
        assertEquals(-32.0, king.getBehaviour<March>()?.step)

        // Phase 3 -> Deal lethal damage to trigger Phase 4 (dismounts on foot, full HP, rains corpses)
        king.hp = 1
        game.damage(king, 10)
        assertTrue(king in game.objects, "King must enter Phase 4 on foot instead of dying in Phase 3")
        assertEquals(GameSprites.THE_KING_ON_FOOT, king.spriteName)
        assertEquals(100, king.hp)
        assertEquals(-16.0, king.getBehaviour<March>()?.step)

        // Phase 4 -> Rains corpses every 300ms
        king.onFrame(301.0)
        assertTrue(game.objects.any { it.isTagged(Tags.CORPSE) }, "Phase 4 should rain corpses from ceiling")

        // Phase 4 -> Final defeat
        game.damage(king, 100)
        assertEquals(0, king.hp)
        assertFalse(king in game.objects)
    }

    @Test
    fun testSeekingFrozenLightningStrikeAndHitStreakBehaviours() {
        // Test Seeking
        val enemy = createVillager()
        game.spawn(enemy, 80.0, 20.0)
        val homingSpell = createSpell().apply {
            x = 20.0
            y = 0.0
            vx = 100.0
            vy = 0.0
        }
        homingSpell.addBehaviour(Seeking(homingSpell))
        game.spawn(homingSpell, 20.0, 0.0)
        homingSpell.onFrame(16.0)
        assertTrue(homingSpell.vy > 0.0, "Seeking spell should steer upward towards target at y=20")

        // Test Frozen
        val frozenEnemy = createVillager()
        val frozen = Frozen(frozenEnemy)
        frozenEnemy.addBehaviour(frozen, 0)
        val initialX = frozenEnemy.x
        for (i in 1..10) {
            frozenEnemy.onUpdate()
        }
        assertEquals(initialX, frozenEnemy.x, "Frozen enemy should not march while frozen")
        assertNull(frozenEnemy.getBehaviour<Frozen>(), "Frozen behaviour should remove itself after 10 turns")

        // Test LightningStrike
        val lightningCarrier = createSpell()
        lightningCarrier.addBehaviour(LightningStrike(lightningCarrier))
        game.spawn(lightningCarrier, 80.0, 20.0)
        lightningCarrier.onCollision(enemy)
        val bolts = game.objects.count { it.spriteName == GameSprites.P_SKULL_YELLOW }
        assertEquals(3, bolts, "LightningStrike should spawn 3 lightning bolts on collision")

        // Test HitStreak hit and miss
        val s1 = createSpell().apply { groupId = 99 }
        s1.addBehaviour(HitStreak(s1))
        game.spawn(s1, 80.0, 20.0)
        s1.onCollision(enemy)
        game.despawn(s1)
        assertEquals(1, game.streak)

        val s2 = createSpell().apply { groupId = 100 }
        s2.addBehaviour(HitStreak(s2))
        game.spawn(s2, 10.0, 10.0)
        game.despawn(s2) // Despawned without hitting anything
        assertEquals(0, game.streak)
    }

    @Test
    fun testMarchTweensScreenshakeAndStageExitDespawn() {
        val heavyUnit = createChampion().apply {
            mass = 100.0
            x = 100.0
            y = 0.0
        }
        game.spawn(heavyUnit, 100.0, 0.0)
        heavyUnit.onUpdate()

        // Advance TweenManager 200ms to complete the march step
        TweenManager.update(100.0)
        assertTrue(heavyUnit.hop > 0.0, "Unit should hop mid-march")
        TweenManager.update(100.0)
        assertEquals(84.0, heavyUnit.x, 1e-4)
        assertEquals(50.0, TweenManager.screenShakeTimer, "Heavy unit landing should trigger screenshake")

        // Unit marching past left edge (< 0) should despawn
        heavyUnit.x = -5.0
        heavyUnit.onUpdate()
        assertFalse(heavyUnit in game.objects)
    }
}
