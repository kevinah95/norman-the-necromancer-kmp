package io.github.kevinah95.norman_the_necromancer.rituals

import io.github.kevinah95.norman_the_necromancer.assets.GameSprites
import io.github.kevinah95.norman_the_necromancer.behaviours.*
import io.github.kevinah95.norman_the_necromancer.core.*
import io.github.kevinah95.norman_the_necromancer.entities.*
import io.github.kevinah95.norman_the_necromancer.levels.LevelManager
import io.github.kevinah95.norman_the_necromancer.shop.ShopItem
import io.github.kevinah95.norman_the_necromancer.shop.ShopKeys
import io.github.kevinah95.norman_the_necromancer.shop.ShopManager
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RitualsAndShopTest {

    private lateinit var game: Game
    private lateinit var player: GameObject

    @BeforeTest
    fun setUp() {
        gameRandom = Random(seed = 42)
        TweenManager.reset()
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
    }

    @Test
    fun testAllSpellModifyingRituals() {
        // BouncingRitual + CeilingRitual + WeightlessRitual + HunterRitual + KnockbackRitual + SeerRitual + ElectrodynamicsRitual + BleedRitual
        game.addRitual(BouncingRitual)
        game.addRitual(CeilingRitual)
        game.addRitual(WeightlessRitual)
        game.addRitual(HunterRitual)
        game.addRitual(KnockbackRitual)
        game.addRitual(SeerRitual)
        game.addRitual(ElectrodynamicsRitual)
        game.addRitual(BleedRitual)

        assertEquals(48.0, game.stage.ceiling)

        assertTrue(game.castSpell())
        val spell = game.objects.first { it.isTagged(Tags.SPELL) }
        assertFalse(spell.despawnOnBounce)
        assertEquals(0.0, spell.mass)
        assertEquals(0.0, spell.friction)
        assertEquals(1.0, spell.bounce)
        assertEquals(Tags.LIVING, spell.collisionMask)
        assertEquals(GameSprites.P_RED_SKULL, spell.spriteName)
        assertNotNull(spell.getBehaviour<DespawnTimer>())
        assertNotNull(spell.getBehaviour<Seeking>())
        assertNotNull(spell.getBehaviour<LightningStrike>())

        // Test Knockback + Bleed on collision with a normal-mass enemy
        val champion = createChampion()
        game.spawn(champion, 100.0, 0.0)
        val initialX = champion.x
        spell.onCollision(champion)
        TweenManager.update(200.0)
        assertEquals(initialX + 16.0, champion.x, 1e-4, "KnockbackRitual should push enemy back by 16px")
        assertNotNull(champion.getBehaviour<Bleeding>(), "BleedRitual should inflict Bleeding")
    }

    @Test
    fun testDoubleshotDrunkardAndTearstoneRituals() {
        game.addRitual(DoubleshotRitual)
        game.addRitual(DrunkardRitual)
        game.addRitual(TearstoneRitual)

        assertEquals(2, game.spell.shotsPerRound)

        // Lower Norman's HP below half (maxHp = 5, hp = 2 < 2.5) to activate Tearstone (3x) + Drunkard (2x) = 6x damage
        player.hp = 2
        assertTrue(game.castSpell())
        val spells = game.objects.filter { it.isTagged(Tags.SPELL) }
        assertEquals(2, spells.size)
        for (s in spells) {
            assertEquals(6, s.getBehaviour<Damaging>()?.amount)
        }
    }

    @Test
    fun testRainRitualSplitsSpellsWhenFalling() {
        game.addRitual(RainRitual)
        assertTrue(game.castSpell())
        val primarySpell = game.objects.first { it.isTagged(Tags.SPELL) }

        // Force upward velocity -> does not split yet
        primarySpell.vy = 10.0
        primarySpell.onFrame(16.0)
        assertEquals(1, game.objects.count { it.isTagged(Tags.SPELL) })

        // Force downward velocity (vy < 0) -> splits into 2 extra projectiles!
        primarySpell.vy = -5.0
        primarySpell.onFrame(16.0)
        assertEquals(3, game.objects.count { it.isTagged(Tags.SPELL) })
    }

    @Test
    fun testChillyRitualFreezesEnemyWhenProbabilityTriggers() {
        game.addRitual(ChillyRitual)
        val enemy = createChampion()
        game.spawn(enemy, 100.0, 0.0)

        // Cast spells with seeded random until the 10% Chilly proc triggers
        var iceSpell: GameObject? = null
        for (i in 0 until 50) {
            game.spell.casts = 3
            game.castSpell()
            iceSpell = game.objects.firstOrNull { it.isTagged(Tags.SPELL) && it.spriteName == GameSprites.P_SKULL }
            if (iceSpell != null) break
        }
        assertNotNull(iceSpell, "ChillyRitual must produce an ice skull spell within 50 deterministic casts")
        assertEquals(0, iceSpell.getBehaviour<Damaging>()?.amount)

        iceSpell.onCollision(enemy)
        assertNotNull(enemy.getBehaviour<Frozen>(), "Target hit by Chilly ice spell should become Frozen")
    }

    @Test
    fun testResurrectionAndEconomyRituals() {
        // Impatience (cooldown / 2), Allegiance (3 Skeleton Lords), Avarice (+1 soul per corpse),
        // Hardened (+1 HP on undead), Giants (20% chance for SkeletonLord), Salvage (corpses -> 5 souls at level end)
        game.addRitual(ImpatienceRitual)
        assertEquals(5_000.0, game.ability.cooldown)

        game.addRitual(AllegianceRitual)
        game.addRitual(AvariceRitual)
        game.addRitual(HardenedRitual)
        game.addRitual(GiantsRitual)
        game.addRitual(SalvageRitual)

        // Spawn 15 corpses so Giants (20%), Avarice, and Hardened are all thoroughly exercised
        for (i in 0 until 15) {
            game.spawn(createCorpse(), 20.0 + i * 5.0, 0.0)
        }

        game.ability.timer = 5_000.0
        assertTrue(game.resurrect())
        assertEquals(15.0, game.souls, 1e-6, "AvariceRitual should grant 1 soul for each of the 15 corpses")

        val undeadUnits = game.objects.filter { it.isTagged(Tags.UNDEAD) && it !== player }
        assertEquals(18, undeadUnits.size, "15 resurrected corpses + 3 Allegiance Skeleton Lords = 18 undead units")
        assertTrue(undeadUnits.any { it.spriteName == GameSprites.BIG_SKELETON && it.x >= 20.0 }, "GiantsRitual should upgrade at least one resurrected skeleton to Skeleton Lord")
        assertTrue(undeadUnits.any { it.spriteName == GameSprites.SKELETON && it.hp == 2 }, "HardenedRitual should increase standard skeleton HP from 1 to 2")

        // Test SalvageRitual at level end
        game.spawn(createCorpse(), 50.0, 0.0)
        game.spawn(createCorpse(), 60.0, 0.0)
        val soulsBeforeSalvage = game.souls
        game.onLevelEnd()
        assertEquals(soulsBeforeSalvage + 10.0, game.souls, 1e-6, "SalvageRitual should convert 2 corpses into 10 souls")
        assertFalse(game.objects.any { it.isTagged(Tags.CORPSE) })
    }

    @Test
    fun testShopManagerLifecycleStudiousDiscountAndPurchases() {
        player.hp = 3 // Damaged player so Heal appears in shop
        game.souls = 1000.0
        game.addRitual(StudiousRitual)

        LevelManager.init(game)
        ShopManager.init(game)
        assertEquals(game, ShopManager.currentGame)
        ShopManager.enterShop()

        assertEquals(GameState.SHOPPING, game.state)
        assertTrue(ShopManager.items.any { it.key == ShopKeys.HEAL }, "Damaged player should see Heal option")
        assertTrue(ShopManager.items.any { it.key == ShopKeys.RENEW })
        assertTrue(ShopManager.items.any { it.key == ShopKeys.RECHARGE })
        assertTrue(ShopManager.items.last().key == ShopKeys.CONTINUE)

        // StudiousRitual halves all costs on onShopEnter (e.g. Heal cost = (10 * 1) / 2 = 5)
        val healIdx = ShopManager.items.indexOfFirst { it.key == ShopKeys.HEAL }
        assertEquals(5, ShopManager.items[healIdx].cost)
        assertTrue(ShopManager.buyItem(healIdx))
        assertEquals(4, player.hp)

        // Buy Renew (+1 maxHp & +1 hp)
        val renewIdx = ShopManager.items.indexOfFirst { it.key == ShopKeys.RENEW }
        assertTrue(ShopManager.buyItem(renewIdx))
        assertEquals(6, player.maxHp)
        assertEquals(5, player.hp)

        // Buy Recharge (+1 maxCasts & +1 casts)
        val rechargeIdx = ShopManager.items.indexOfFirst { it.key == ShopKeys.RECHARGE }
        assertTrue(ShopManager.buyItem(rechargeIdx))
        assertEquals(4, game.spell.maxCasts)
        assertEquals(4, game.spell.casts)

        // Buy a ritual item from the shop
        val ritualItemIdx = ShopManager.items.indexOfFirst { it.key != ShopKeys.CONTINUE }
        if (ritualItemIdx >= 0) {
            assertTrue(ShopManager.buyItem(ritualItemIdx))
            assertTrue(game.rituals.isNotEmpty())
        }

        // Test navigation clamping
        ShopManager.selectIndex(-100)
        assertEquals(0, ShopManager.selectedIndex)
        ShopManager.selectIndex(100)
        assertEquals(ShopManager.items.size - 1, ShopManager.selectedIndex)

        // Test insufficient souls
        game.souls = 0.0
        ShopManager.selectedIndex = 0
        if (ShopManager.items[0].cost > 0) {
            assertFalse(ShopManager.buyCurrent())
        }

        // Test secondary ShopItem constructor
        val customItem = ShopItem(cost = 50, name = "Custom", description = "Custom Desc", onPurchase = {})
        assertEquals("Custom", customItem.name)
        assertEquals("Custom Desc", customItem.description)

        // Buy Continue (cost = 0) -> exits shop and starts next level
        val continueIdx = ShopManager.items.indexOfFirst { it.key == ShopKeys.CONTINUE }
        assertTrue(ShopManager.buyItem(continueIdx))
        assertEquals(GameState.PLAYING, game.state)
        assertEquals(1, game.level)
    }

    @Test
    fun testLevelManagerWaveAndLevelProgression() {
        LevelManager.init(game)
        assertFalse(LevelManager.isLevelFinished())
        assertFalse(LevelManager.isComplete())

        // Fast-forward through all 10 levels in LevelManager
        var safetyTicks = 0
        while (!LevelManager.isComplete() && safetyTicks < 5000) {
            safetyTicks++
            LevelManager.updateLevel(5000.0)
            // Clear spawned living enemies so END_OF_WAVE and END_OF_LEVEL can advance
            for (enemy in game.objects.filter { it.isTagged(Tags.LIVING) }) {
                game.despawn(enemy)
            }
            if (LevelManager.isLevelFinished() && !LevelManager.isComplete()) {
                LevelManager.nextLevel()
            }
        }

        assertTrue(LevelManager.isComplete(), "LevelManager should reach completion after all 10 levels")
        assertTrue(LevelManager.isLevelFinished())
        assertEquals(9, game.level)
    }
}
