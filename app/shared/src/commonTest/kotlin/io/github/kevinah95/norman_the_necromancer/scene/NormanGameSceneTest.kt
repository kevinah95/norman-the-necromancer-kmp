package io.github.kevinah95.norman_the_necromancer.scene

import io.github.kevinah95.norman_the_necromancer.assets.GameSprites
import io.github.kevinah95.norman_the_necromancer.audio.GameAudio
import io.github.kevinah95.norman_the_necromancer.core.*
import io.github.kevinah95.norman_the_necromancer.entities.createVillager
import io.github.kevinah95.norman_the_necromancer.i18n.GameLanguage
import io.github.kevinah95.norman_the_necromancer.i18n.GameStrings
import io.github.kevinah95.norman_the_necromancer.shop.ShopKeys
import io.github.kevinah95.norman_the_necromancer.shop.ShopManager
import korlibs.event.Key
import korlibs.korge.scene.sceneContainer
import korlibs.korge.tests.ViewsForTesting
import korlibs.math.geom.Size
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NormanGameSceneTest : ViewsForTesting(
    virtualSize = Size(GameLayout.VIRTUAL_WIDTH_INT, GameLayout.VIRTUAL_HEIGHT_INT),
    windowSize = Size(GameLayout.VIRTUAL_WIDTH_INT, GameLayout.VIRTUAL_HEIGHT_INT)
) {

    @BeforeTest
    fun setUp() {
        gameRandom = Random(seed = 42)
        GameAudio.enabled = false
        GameStrings.language = GameLanguage.EN
        TweenManager.reset()
    }

    @AfterTest
    fun tearDown() {
        gameRandom = Random.Default
        GameAudio.enabled = true
        GameStrings.language = GameLanguage.EN
        TweenManager.reset()
    }

    @Test
    fun `al hacer click lanza hechizo y al presionar espacio resucita enemigos caidos`() = viewsTest {
        val sc = sceneContainer()
        val scene = sc.changeTo { NormanGameScene() }

        // Transition directly from INTRO to PLAYING by confirming intro lines
        scene.dialogueCooldown = 0.0
        while (scene.game.state == GameState.INTRO) {
            scene.dialogueCooldown = 0.0
            keyDownThenUp(Key.SPACE)
        }
        assertEquals(GameState.PLAYING, scene.game.state)
        assertEquals(GameSprites.NORMAN_ARMS_DOWN, scene.game.player.spriteName)

        // Clear any auto-spawned wave enemies and place a deterministic villager with 100% corpse drop in front of Norman
        scene.game.objects.removeAll { it.isTagged(Tags.LIVING) }
        val targetVillager = createVillager().apply {
            hp = 1
            corpseChance = 1.0
        }
        scene.game.spawn(targetVillager, 60.0, 0.0)

        // 1. Aim and click at (60, 142) in screen coordinates (which corresponds to world y ~ 8)
        mouseMoveTo(60, 142)
        mouseDown()
        assertTrue(scene.touchAimActive, "Touch/mouse aiming should activate on pointer down")
        mouseUp()
        assertFalse(scene.touchAimActive, "Touch/mouse aiming should release and fire spell on pointer up")
        assertEquals(2, scene.game.spell.casts, "Casting a spell should consume 1 cast")
        assertTrue(scene.game.objects.any { it.isTagged(Tags.SPELL) }, "Spell projectile must be spawned")

        // 2. Advance frames until projectile impacts the villager and spawns a corpse
        for (i in 0 until 30) {
            scene.game.update(16.0)
        }
        assertTrue(
            scene.game.objects.any { it.isTagged(Tags.CORPSE) },
            "El enemigo debería haber muerto y dejado un cadáver"
        )

        // 3. Simulate clicking RESURRECT button or pressing SPACE for the resurrection ritual
        scene.game.ability.timer = scene.game.ability.cooldown
        mouseMoveAndClickTo(GameLayout.RESURRECT_BUTTON.centerX, GameLayout.RESURRECT_BUTTON.centerY)
        delayFrame()

        // 4. Verify the corpse is now an allied skeleton in Norman's army
        val aliveUndeadMinions = scene.game.objects.count { it.isTagged(Tags.UNDEAD) && it !== scene.game.player }
        assertEquals(1, aliveUndeadMinions, "El cadáver debe convertirse en un esqueleto aliado de Norman")

        // Also verify SPACE key resurrection works when another corpse is present
        scene.game.spawn(io.github.kevinah95.norman_the_necromancer.entities.createCorpse(), 80.0, 0.0)
        scene.game.ability.timer = scene.game.ability.cooldown
        keyDownThenUp(Key.SPACE)
        delayFrame()
        assertEquals(2, scene.game.objects.count { it.isTagged(Tags.UNDEAD) && it !== scene.game.player })
    }

    @Test
    fun `cuando el juego esta en pausa avanzar el tiempo no mueve enemigos ni proyectiles`() = viewsTest {
        val sc = sceneContainer()
        val scene = sc.changeTo { NormanGameScene() }

        scene.game.state = GameState.PLAYING
        val enemy = createVillager()
        scene.game.spawn(enemy, 200.0, 0.0)
        scene.game.castSpell()
        val spell = scene.game.objects.first { it.isTagged(Tags.SPELL) }

        // Pause the game using Key.P
        keyDownThenUp(Key.P)
        assertTrue(scene.isPaused, "Game should be paused after pressing P")

        val enemyXBefore = enemy.x
        val enemyYBefore = enemy.y
        val spellXBefore = spell.x
        val spellYBefore = spell.y

        // Advance several virtual frames while paused
        for (i in 0 until 20) {
            delayFrame()
        }

        // Verify neither enemies nor projectiles moved while paused
        assertEquals(enemyXBefore, enemy.x, 1e-6, "Enemy X must not change while paused")
        assertEquals(enemyYBefore, enemy.y, 1e-6, "Enemy Y must not change while paused")
        assertEquals(spellXBefore, spell.x, 1e-6, "Spell X must not change while paused")
        assertEquals(spellYBefore, spell.y, 1e-6, "Spell Y must not change while paused")

        // Unpause via Key.SPACE and verify pause button touch hitbox also works
        keyDownThenUp(Key.SPACE)
        assertFalse(scene.isPaused, "Pressing SPACE while paused should unpause the game")

        keyDownThenUp(Key.P)
        assertTrue(scene.isPaused)
        keyDownThenUp(Key.ESCAPE)
        assertFalse(scene.isPaused, "Pressing ESCAPE should unpause the game")

        mouseMoveAndClickTo(GameLayout.PAUSE_BUTTON.centerX, GameLayout.PAUSE_BUTTON.centerY)
        assertTrue(scene.isPaused, "Clicking pause button should pause the game")

        // Clicking anywhere while paused unpauses
        mouseMoveAndClickTo(100, 100)
        assertFalse(scene.isPaused, "Clicking screen while paused should resume the game")
    }

    @Test
    fun `navegacion de la tienda con teclado y raton compra mejoras y avanza al siguiente nivel`() = viewsTest {
        val sc = sceneContainer()
        val scene = sc.changeTo { NormanGameScene() }

        scene.game.souls = 500.0
        ShopManager.enterShop()
        assertEquals(GameState.SHOPPING, scene.game.state)
        assertEquals(0, ShopManager.selectedIndex)

        // Navigate down and up with keyboard arrows
        keyDownThenUp(Key.DOWN)
        assertEquals(1, ShopManager.selectedIndex)
        keyDownThenUp(Key.UP)
        assertEquals(0, ShopManager.selectedIndex)

        // Purchase first item (Renew: +1 Max HP) with Key.ENTER
        val initialMaxHp = scene.game.player.maxHp
        keyDownThenUp(Key.ENTER)
        assertEquals(initialMaxHp + 1, scene.game.player.maxHp)

        // Select an item via mouse/touch click and tap the Buy/Continue action zone
        val continueIdx = ShopManager.items.indexOfFirst { it.key == ShopKeys.CONTINUE }
        val continueY = GameLayout.SHOP_ITEM_START_Y + continueIdx * GameLayout.SHOP_ITEM_HEIGHT + 4.0
        mouseMoveAndClickTo(200.0, continueY)
        assertEquals(continueIdx, ShopManager.selectedIndex)

        val actionButtonY = GameLayout.SHOP_ITEM_START_Y + ShopManager.items.size * GameLayout.SHOP_ITEM_HEIGHT + 22.0
        mouseMoveAndClickTo(200.0, actionButtonY)
        assertEquals(GameState.PLAYING, scene.game.state, "Purchasing Continue should exit shop and resume PLAYING")
        assertEquals(1, scene.game.level)

        // Advance to level 2 and verify Norman's Easter Egg bounce activates
        scene.game.level = 2
        delayFrame()
        assertTrue(scene.normanIsBouncing, "Norman should start bouncing to the beat on Level 2")
    }

    @Test
    fun `cambio de idioma en intro atajo de victoria y reinicio tras creditos`() = viewsTest {
        val sc = sceneContainer()
        val scene = sc.changeTo { NormanGameScene() }

        assertEquals(GameState.INTRO, scene.game.state)
        assertEquals(GameLanguage.EN, GameStrings.language)

        // Click language button on intro screen
        mouseMoveAndClickTo(GameLayout.LANG_BUTTON.centerX, GameLayout.LANG_BUTTON.centerY)
        assertEquals(GameLanguage.ES, GameStrings.language)
        assertTrue(scene.game.dialogue[0].contains("nigromante"))

        // Toggle back to English with Key.L
        keyDownThenUp(Key.L)
        assertEquals(GameLanguage.EN, GameStrings.language)

        // Test auto-advancing dialogue timer on INTRO
        val initialIntroLines = scene.game.dialogue.size
        scene.dialogueTimer = 6000.0
        delayFrame()
        assertEquals(initialIntroLines - 1, scene.game.dialogue.size)

        // Click to advance intro dialogue
        scene.dialogueCooldown = 0.0
        mouseMoveAndClickTo(100, 100)

        // Start playing and test WIN_SHORTCUT_TOUCH_BOUNDS
        scene.game.state = GameState.PLAYING
        scene.game.dialogue.clear()
        scene.game.dialogue.add("In-game line")
        scene.dialogueTimer = 6000.0
        delayFrame()
        assertTrue(scene.game.dialogue.isEmpty(), "In-game dialogue should clear after timer expires")

        mouseMoveAndClickTo(GameLayout.WIN_SHORTCUT.centerX, GameLayout.WIN_SHORTCUT.centerY)
        assertEquals(GameState.WIN, scene.game.state)
        assertEquals(5, scene.game.dialogue.size)

        // Also verify Key.W shortcut from PLAYING state
        scene.game.state = GameState.PLAYING
        keyDownThenUp(Key.W)
        assertEquals(GameState.WIN, scene.game.state)

        // Toggle language during WIN state
        keyDownThenUp(Key.L)
        assertEquals(GameLanguage.ES, GameStrings.language)
        keyDownThenUp(Key.L)
        assertEquals(GameLanguage.EN, GameStrings.language)

        // Auto-advance timer in WIN state (> 8000ms)
        scene.dialogueTimer = 8500.0
        delayFrame()
        assertEquals(4, scene.game.dialogue.size)

        // Click during WIN state
        scene.dialogueCooldown = 0.0
        mouseMoveAndClickTo(100, 100)
        assertEquals(3, scene.game.dialogue.size)

        // Advance through remaining outro screens until the game restarts back to INTRO
        while (scene.game.state == GameState.WIN) {
            scene.dialogueCooldown = 0.0
            keyDownThenUp(Key.ENTER)
        }
        assertEquals(GameState.INTRO, scene.game.state, "Confirming final outro screen should restart game to INTRO")

        // Test LOSE state recovery via update tick, in-frame collision death, click, and confirm action
        scene.game.state = GameState.LOSE
        mouseDown()
        mouseUp()
        assertEquals(GameState.INTRO, scene.game.state)

        scene.game.state = GameState.LOSE
        delayFrame()
        assertEquals(GameState.INTRO, scene.game.state)

        scene.game.state = GameState.PLAYING
        scene.game.player.hp = 0
        delayFrame()
        assertEquals(GameState.INTRO, scene.game.state)

        // Enemy colliding with Norman during PLAYING update tick triggers onLose after game.update(dtMs)
        scene.game.state = GameState.PLAYING
        keyDownThenUp(Key.ENTER) // Confirm action during PLAYING is a safe no-op
        val lethalEnemy = createVillager().apply { hp = 10 }
        scene.game.spawn(lethalEnemy, scene.game.player.x, 0.0)
        delayFrame()
        assertEquals(GameState.INTRO, scene.game.state)

        // Test checkLevelProgression transitioning to SHOPPING on level end and to WIN on game completion
        scene.game.state = GameState.PLAYING
        var guard = 0
        while (!io.github.kevinah95.norman_the_necromancer.levels.LevelManager.isLevelFinished() && guard < 200) {
            guard++
            io.github.kevinah95.norman_the_necromancer.levels.LevelManager.updateLevel(5000.0)
            scene.game.objects.removeAll { it.isTagged(Tags.LIVING) }
        }
        delayFrame()
        assertEquals(GameState.SHOPPING, scene.game.state, "Completing Level 1 should automatically enter the shop")

        // Fast-forward LevelManager to the final level's END_OF_LEVEL and verify checkLevelProgression calls onWin()
        ShopManager.exitShop()
        while (!io.github.kevinah95.norman_the_necromancer.levels.LevelManager.isComplete() && guard < 5000) {
            guard++
            io.github.kevinah95.norman_the_necromancer.levels.LevelManager.updateLevel(5000.0)
            scene.game.objects.removeAll { it.isTagged(Tags.LIVING) }
            if (io.github.kevinah95.norman_the_necromancer.levels.LevelManager.isLevelFinished() &&
                !io.github.kevinah95.norman_the_necromancer.levels.LevelManager.isComplete()
            ) {
                io.github.kevinah95.norman_the_necromancer.levels.LevelManager.nextLevel()
            }
        }
        scene.game.objects.removeAll { it.isTagged(Tags.LIVING) }
        delayFrame()
        assertEquals(GameState.WIN, scene.game.state, "Completing final level should automatically trigger WIN state")

        scene.game.state = GameState.LOSE
        keyDownThenUp(Key.ENTER)
        assertEquals(GameState.INTRO, scene.game.state)

        scene.sceneDestroy()
    }
}
