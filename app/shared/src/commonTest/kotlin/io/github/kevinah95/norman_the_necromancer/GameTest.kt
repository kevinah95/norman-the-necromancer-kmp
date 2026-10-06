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
        assertTrue(GameAtlas.glyphSlices.size >= 96)
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

    @Test
    fun testPlayerSpellsDoNotDamageResurrectedSkeletons() {
        val player = createPlayer()
        val game = Game().apply {
            this.player = player
            spawn(player)
            state = GameState.PLAYING
        }
        val skeleton = createSkeleton().apply {
            x = 50.0
            y = 0.0
        }
        game.spawn(skeleton)

        val spell = createSpell().apply {
            x = 50.0
            y = 0.0
            vx = 10.0
            vy = 0.0
        }
        game.spawn(spell)

        // Ensure both overlap
        assertTrue(spell.bounds().overlaps(skeleton.bounds()))

        // Simulate game step
        game.update(16.0)

        // Resurrected skeleton should NOT take damage or die
        assertEquals(1, skeleton.hp)
        assertTrue(skeleton in game.objects)

        // Spell should NOT despawn on collision with the skeleton
        assertTrue(spell in game.objects)
    }

    @Test
    fun testPlayerSpellsDamageLivingEnemies() {
        val player = createPlayer()
        val game = Game().apply {
            this.player = player
            spawn(player)
            state = GameState.PLAYING
        }
        val villager = createVillager().apply {
            x = 50.0
            y = 0.0
            hp = 1
        }
        game.spawn(villager)

        val spell = createSpell().apply {
            x = 50.0
            y = 0.0
            vx = 10.0
            vy = 0.0
        }
        game.spawn(spell)

        assertTrue(spell.bounds().overlaps(villager.bounds()))

        // Simulate game step
        game.update(16.0)

        // Living enemy should be damaged and killed
        assertEquals(0, villager.hp)
        assertTrue(villager !in game.objects)

        // Spell should despawn on collision with enemy
        assertTrue(spell !in game.objects)
    }

    @Test
    fun testTextWidthCalculation() = suspendTest {
        GameAtlas.load()
        val renderer = io.github.kevinah95.norman_the_necromancer.renderer.GameRenderer()
        val singleLineWidth = renderer.getTextWidth("THE END")
        assertTrue(singleLineWidth > 0.0)

        val multiLineWidth = renderer.getTextWidth("Original Game by Dan Prince\nCreated for JS13k Games 2022\ndanthedev.com")
        assertTrue(multiLineWidth >= singleLineWidth)
    }

    @Test
    fun testOutroDialogueSequence() {
        val outroDialogue = listOf(
            "It was over.",
            "Norman was able to study peacefully.",
            "But he knew that eventually, they'd be back.",
            "THE END",
            "Original Game by Dan Prince\nCreated for JS13k Games 2022\ndanthedev.com\n\nThanks for playing!"
        )

        val game = Game().apply {
            state = GameState.WIN
            dialogue.addAll(outroDialogue)
        }

        assertEquals(5, game.dialogue.size)
        assertEquals("It was over.", game.dialogue[0])

        // Advance through dialogue
        while (game.dialogue.size > 1) {
            game.dialogue.removeAt(0)
        }

        // Final screen remains with credits and Thanks for playing
        assertEquals(1, game.dialogue.size)
        assertTrue(game.dialogue[0].contains("Dan Prince"))
        assertTrue(game.dialogue[0].contains("Thanks for playing!"))
    }

    @Test
    fun testLocalizationAndLanguageToggle() {
        val strings = io.github.kevinah95.norman_the_necromancer.i18n.GameStrings
        strings.language = io.github.kevinah95.norman_the_necromancer.i18n.GameLanguage.EN

        assertEquals("PAUSE", strings.hudPause)
        assertEquals("Bouncing", strings.getRitualName("Bouncing"))
        assertEquals("Spells bounce", strings.getRitualDesc("Bouncing", "Spells bounce"))
        assertEquals(5, strings.getIntroDialogue().size)

        // Toggle to Spanish
        val newLang = strings.toggleLanguage()
        assertEquals(io.github.kevinah95.norman_the_necromancer.i18n.GameLanguage.ES, newLang)
        assertEquals("PAUSA", strings.hudPause)
        assertEquals("Rebote", strings.getRitualName("Bouncing"))
        assertEquals("Los hechizos rebotan", strings.getRitualDesc("Bouncing", "Spells bounce"))
        assertEquals("Vitalidad", strings.getRitualName("Renew"))
        assertEquals("+1 Vida Maxima", strings.getRitualDesc("Renew", "+1 Max HP"))
        assertEquals("Carga Magica", strings.getRitualName("Recharge"))
        assertEquals("+1 Hechizo Maximo", strings.getRitualDesc("Recharge", "+1 Max Cast"))
        assertEquals("Continuar", strings.getRitualName("Continue"))
        assertEquals("Comenzar siguiente nivel", strings.getRitualDesc("Continue", "Begin next level"))
        assertTrue(strings.getIntroDialogue()[0].contains("Norman"))
        assertTrue(strings.getOutroDialogue()[3] == "FIN")

        // Reset to English
        strings.toggleLanguage()
        assertEquals(io.github.kevinah95.norman_the_necromancer.i18n.GameLanguage.EN, strings.language)
    }

    @Test
    fun testAccentedCharactersFontMetrics() = suspendTest {
        GameAtlas.load()
        val renderer = io.github.kevinah95.norman_the_necromancer.renderer.GameRenderer()
        // Spanish text with accents should measure without error and have positive width
        val width = renderer.getTextWidth("Había terminado. ¡Gracias por jugar!")
        assertTrue(width > 0.0)
    }

    @Test
    fun testGameLayoutAndGeometryHelpers() {
        val rect = Rect2D(10.0, 20.0, 30.0, 40.0)
        assertEquals(25.0, rect.centerX)
        assertEquals(40.0, rect.centerY)
        assertEquals(40.0, rect.right)
        assertEquals(60.0, rect.bottom)

        val expanded = rect.expanded(5.0)
        assertEquals(5.0, expanded.x)
        assertEquals(15.0, expanded.y)
        assertEquals(40.0, expanded.w)
        assertEquals(50.0, expanded.h)

        assertTrue(GameLayout.VIRTUAL_WIDTH == 400.0)
        assertTrue(GameLayout.VIRTUAL_HEIGHT == 200.0)
        assertTrue(GameLayout.PAUSE_TOUCH_BOUNDS.contains(GameLayout.PAUSE_BUTTON.centerX, GameLayout.PAUSE_BUTTON.centerY))
        assertTrue(GameLayout.LANG_TOUCH_BOUNDS.contains(GameLayout.LANG_BUTTON.centerX, GameLayout.LANG_BUTTON.centerY))
    }

    @Test
    fun testCollisionCheckingMasks() {
        val player = createPlayer()
        val enemy = createVillager()
        val spell = createSpell()

        // Spell has collisionMask = Tags.LIVING, enemy has tags = Tags.LIVING
        assertTrue(spell.canCollideWith(enemy))
        // Spell should not collide with itself
        assertTrue(!spell.canCollideWith(spell))
        // Player has tags = Tags.PLAYER, spell has collisionMask = Tags.LIVING
        assertTrue(!spell.canCollideWith(player))
    }

    @Test
    fun testKorGeInteropAndEasing() {
        val pt = Point2D(12.0, 34.0)
        val kPt = pt.toKorGe()
        assertEquals(12.0, kPt.x)
        assertEquals(34.0, kPt.y)
        assertEquals(pt, kPt.toPoint2D())

        val r = Rect2D(10.0, 20.0, 30.0, 40.0)
        val kR = r.toKorGe()
        assertEquals(10.0, kR.x)
        assertEquals(20.0, kR.y)
        assertEquals(30.0, kR.width)
        assertEquals(40.0, kR.height)
        assertEquals(r, kR.toRect2D())

        var progressReceived = 0.0
        TweenManager.reset()
        TweenManager.tween(0.0, 100.0, 100.0, korlibs.math.interpolation.Easing.SMOOTH) { _, progress ->
            progressReceived = progress
        }
        TweenManager.update(50.0)
        assertTrue(progressReceived in 0.0..1.0)
        TweenManager.reset()
    }

    @Test
    fun testSpellUpwardTrajectory() {
        val player = createPlayer()
        val game = Game().apply {
            this.player = player
            spawn(player)
            spell.targetAngle = DEG_90 // 90 degrees, straight up!
            castSpell()
        }
        val spell = game.objects.find { it.tags == Tags.SPELL }
        for (i in 0 until 120) {
            game.update(16.6)
        }
    }

    @Test
    fun testRitualsLocalizationAndKeys() {
        val allRituals = io.github.kevinah95.norman_the_necromancer.rituals.getAllShopRituals()
        assertEquals(20, allRituals.size)

        // Verify English localization
        io.github.kevinah95.norman_the_necromancer.i18n.GameStrings.language = io.github.kevinah95.norman_the_necromancer.i18n.GameLanguage.EN
        val bouncingEn = io.github.kevinah95.norman_the_necromancer.rituals.BouncingRitual
        assertEquals("Bouncing", bouncingEn.name)
        assertEquals("Spells bounce", bouncingEn.description)

        for (ritual in allRituals) {
            assertTrue(ritual.key.isNotBlank(), "Ritual key should not be blank")
            assertTrue(ritual.name.isNotBlank(), "Ritual ${ritual.key} English name should not be blank")
            assertTrue(ritual.description.isNotBlank(), "Ritual ${ritual.key} English description should not be blank")
        }

        // Verify Spanish localization dynamic update
        io.github.kevinah95.norman_the_necromancer.i18n.GameStrings.language = io.github.kevinah95.norman_the_necromancer.i18n.GameLanguage.ES
        val bouncingEs = io.github.kevinah95.norman_the_necromancer.rituals.BouncingRitual
        assertEquals("Rebote", bouncingEs.name)
        assertEquals("Los hechizos rebotan", bouncingEs.description)

        for (ritual in allRituals) {
            assertTrue(ritual.name.isNotBlank(), "Ritual ${ritual.key} Spanish name should not be blank")
            assertTrue(ritual.description.isNotBlank(), "Ritual ${ritual.key} Spanish description should not be blank")
        }

        // Reset to default
        io.github.kevinah95.norman_the_necromancer.i18n.GameStrings.language = io.github.kevinah95.norman_the_necromancer.i18n.GameLanguage.EN
    }
}



