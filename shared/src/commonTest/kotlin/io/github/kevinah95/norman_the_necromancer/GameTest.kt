package io.github.kevinah95.norman_the_necromancer

import io.github.kevinah95.norman_the_necromancer.core.*
import io.github.kevinah95.norman_the_necromancer.entities.*
import io.github.kevinah95.norman_the_necromancer.levels.LevelManager
import io.github.kevinah95.norman_the_necromancer.rituals.StreakRitual
import io.github.kevinah95.norman_the_necromancer.shop.ShopManager
import io.github.kevinah95.norman_the_necromancer.assets.GameAtlas
import korlibs.io.async.suspendTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameTest {

    @Test
    fun testGameAtlasLoad() = suspendTest {
        GameAtlas.load()
        assertTrue(GameAtlas.sprites.isNotEmpty())
        assertTrue(GameAtlas.getSprite("norman_arms_up") != null)
        assertTrue(GameAtlas.glyphSlices.isNotEmpty())
    }

    @Test
    fun testAudioSynthesisAndStream() = suspendTest {
        val stream = io.github.kevinah95.norman_the_necromancer.audio.NormanMusicStream(44100)
        assertTrue(stream.kickBuffer.isNotEmpty())
        assertTrue(stream.organBuffer.isNotEmpty())
        assertTrue(stream.bassBuffer.isNotEmpty())
        assertTrue(stream.kingBuffer.isNotEmpty())

        val samples = korlibs.audio.sound.AudioSamples(2, 512)
        val readCount = stream.read(samples, 0, 512)
        assertEquals(512, readCount)
    }

    @Test
    fun testGameInitializationAndSpells() {
        val player = createPlayer()
        val game = Game().apply {
            this.player = player
            spawn(player)
            addRitual(StreakRitual)
        }

        assertEquals(3, game.spell.casts)
        assertEquals(5, game.player.hp)

        // Test casting
        val castSuccess = game.castSpell()
        assertTrue(castSuccess)
        assertEquals(2, game.spell.casts)

        // Fast forward recharge timer
        game.update(1000.0)
        assertEquals(3, game.spell.casts)
    }

    @Test
    fun testCorpseAndResurrection() {
        val player = createPlayer()
        val game = Game().apply {
            this.player = player
            spawn(player)
        }

        // Spawn a corpse
        val corpse = createCorpse()
        game.spawn(corpse, 100.0, 0.0)
        assertTrue(game.objects.contains(corpse))

        // Trigger resurrection
        val resurrectSuccess = game.resurrect()
        assertTrue(resurrectSuccess)

        // Corpse should be despawned and a skeleton should be spawned
        assertTrue(!game.objects.contains(corpse))
        val skeleton = game.objects.firstOrNull { it.isTagged(Tags.UNDEAD) && it !== player }
        assertTrue(skeleton != null)
    }

    @Test
    fun testShopPurchase() {
        val player = createPlayer()
        val game = Game().apply {
            this.player = player
            spawn(player)
            souls = 500.0
        }
        ShopManager.init(game)
        ShopManager.restockShop()

        assertTrue(ShopManager.items.isNotEmpty())
        val initialSouls = game.souls
        val firstItem = ShopManager.items[0]
        val cost = firstItem.cost

        val bought = ShopManager.buyCurrent()
        assertTrue(bought)
        assertEquals(initialSouls - cost, game.souls)
    }

    @Test
    fun testLevelScriptResetsOnInit() {
        val player = createPlayer()
        val game = Game().apply {
            this.player = player
            spawn(player)
            state = GameState.PLAYING
        }
        LevelManager.init(game)
        assertEquals(4, LevelManager.levelScript[0])

        // Fast forward to spawn enemies and decrement count
        for (i in 0 until 4) {
            LevelManager.updateLevel(1000.0)
        }
        assertEquals(0, LevelManager.levelScript[0])

        // Re-initializing LevelManager must restore original counts
        LevelManager.init(game)
        assertEquals(4, LevelManager.levelScript[0])
    }

    @Test
    fun testIsLevelFinishedRequiresAlivePlayer() {
        val player = createPlayer()
        val game = Game().apply {
            this.player = player
            spawn(player)
            state = GameState.PLAYING
        }
        LevelManager.init(game)

        // Kill Norman
        game.damage(player, 10)
        assertEquals(0, player.hp)
        assertEquals(GameState.LOSE, game.state)

        // Even if no living enemies remain, isLevelFinished must be false when player is dead
        assertTrue(!LevelManager.isLevelFinished())
    }
}
