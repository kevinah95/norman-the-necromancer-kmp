package io.github.kevinah95.norman_the_necromancer.scene

import io.github.kevinah95.norman_the_necromancer.assets.GameAtlas
import io.github.kevinah95.norman_the_necromancer.audio.GameAudio
import io.github.kevinah95.norman_the_necromancer.behaviours.March
import io.github.kevinah95.norman_the_necromancer.core.*
import io.github.kevinah95.norman_the_necromancer.entities.createPlayer
import io.github.kevinah95.norman_the_necromancer.fx.Fx
import io.github.kevinah95.norman_the_necromancer.levels.LevelManager
import io.github.kevinah95.norman_the_necromancer.renderer.GameRenderer
import io.github.kevinah95.norman_the_necromancer.rituals.StreakRitual
import io.github.kevinah95.norman_the_necromancer.shop.ShopManager
import korlibs.event.Key
import korlibs.korge.input.keys
import korlibs.korge.input.mouse
import korlibs.korge.input.onDown
import korlibs.korge.input.onMove
import korlibs.korge.input.onUp
import korlibs.korge.render.RenderContext
import korlibs.korge.scene.Scene
import korlibs.korge.view.SContainer
import korlibs.korge.view.View
import korlibs.korge.view.addUpdater
import korlibs.time.milliseconds
import kotlin.math.atan2

class NormanGameScene : Scene() {
    private val renderer = GameRenderer()
    private lateinit var game: Game
    private var isPaused = false
    private var touchAimActive = false
    private var dialogueTimer = 0.0
    private var normanIsBouncing = false
    private var introCooldown = 0.0

    private val introDialogue = listOf(
        "Norman wasn't a particularly popular necromancer...",
        "         The other villagers hunted him.",
        "     Sometimes they even finished the job.",
        "  But like any self-respecting necromancer...",
        "        Norman just brought himself back.",
        "             (Tap anywhere to begin)"
    )

    private val outroDialogue = listOf(
        "It was over.",
        "Norman was able to study peacefully.",
        "But he knew that eventually, they'd be back.",
        "THE END! (Tap to play again)"
    )

    override suspend fun SContainer.sceneMain() {
        GameAtlas.load()
        initGame()
        GameAudio.play()

        // Dedicated rendering view that draws all layers with KorGE's BatchBuilder2D
        val gameView = object : View() {
            override fun renderInternal(ctx: RenderContext) {
                ctx.useBatcher { batch ->
                    renderer.render(batch, ctx, game, isPaused, touchAimActive)
                }
            }
        }
        addChild(gameView)

        // Main game update loop
        addUpdater { dt ->
            val dtMs = dt.milliseconds
            update(dtMs)
        }

        // Setup touch and mouse controls
        setupInput()
    }

    private fun initGame() {
        val player = createPlayer()
        player.spriteName = "skull" // Starts as a skull in the intro!
        game = Game().apply {
            this.player = player
            spawn(player)
            dialogue.addAll(introDialogue)
            addRitual(StreakRitual)
        }
        ShopManager.init(game)
        LevelManager.init(game)
        TweenManager.reset()
        dialogueTimer = 0.0
        normanIsBouncing = false
        isPaused = false
        touchAimActive = false
        introCooldown = 500.0
        Fx.activeEmitters.clear()
        Fx.dust(game.stage.width, game.stage.height).burst(150)
    }

    private fun update(dtMs: Double) {
        if (introCooldown > 0.0) {
            introCooldown -= dtMs
        }
        updateDialogue(dtMs)
        if (isPaused) return

        if (game.state == GameState.PLAYING) {
            if (game.player.hp <= 0) {
                onLose()
                return
            }
            LevelManager.updateLevel(dtMs)
        } else if (game.state == GameState.LOSE) {
            onLose()
            return
        }

        if (game.state != GameState.INTRO) {
            game.update(dtMs)
            if (game.state == GameState.LOSE || (game.state == GameState.PLAYING && game.player.hp <= 0)) {
                onLose()
                return
            }
        }

        TweenManager.update(dtMs)
        Fx.update(dtMs)

        if (game.state == GameState.PLAYING && LevelManager.isLevelFinished()) {
            if (LevelManager.isComplete()) {
                onWin()
            } else {
                game.onLevelEnd()
                ShopManager.enterShop()
            }
        }

        if (game.level == 2 && !normanIsBouncing && game.state == GameState.PLAYING) {
            game.player.addBehaviour(March(game.player, 0.0))
            game.player.updateClock = 100.0
            game.player.updateSpeed = (60_000.0 / 240.0) * 2.0
            normanIsBouncing = true
        }
    }

    private fun onWin() {
        game.state = GameState.WIN
        game.dialogue.clear()
        game.dialogue.addAll(outroDialogue)
    }

    private fun onLose() {
        initGame()
        GameAudio.useLevelSynths(game.level)
    }

    private fun updateDialogue(dtMs: Double) {
        dialogueTimer += dtMs
        if (dialogueTimer > 4000.0) {
            dialogueTimer = 0.0
            if (game.dialogue.isNotEmpty()) {
                game.dialogue.removeAt(0)
            }
            if (game.state == GameState.INTRO && game.dialogue.isEmpty()) {
                game.dialogue.add("                (Tap to begin)")
            }
        }
    }

    private fun SContainer.setupInput() {
        // Touch / pointer down
        onDown { evt ->
            val pos = evt.currentPosLocal
            val px = pos.x
            val py = pos.y

            when (game.state) {
                GameState.INTRO -> {
                    if (introCooldown <= 0.0) {
                        game.state = GameState.PLAYING
                        game.player.spriteName = "norman_arms_down"
                        game.dialogue.clear()
                        GameAudio.play()
                        GameAudio.useLevelSynths(game.level)
                    }
                }
                GameState.WIN, GameState.LOSE -> {
                    onLose()
                }
                GameState.PLAYING -> {
                    // Check Pause button tap (top-right: x >= 370, y <= 25)
                    if (px >= 370.0 && py <= 25.0) {
                        isPaused = !isPaused
                        return@onDown
                    }

                    // Check Resurrect button tap (bottom center: x in 130..270, y in 170..200)
                    if (px in 130.0..270.0 && py in 170.0..200.0) {
                        if (game.resurrect()) {
                            GameAudio.playResurrect()
                        }
                        return@onDown
                    }

                    // Touch aim on battlefield
                    touchAimActive = true
                    updateAimAngle(px, py)
                }
                GameState.SHOPPING -> {
                    // Check item list selection
                    var itemY = 40.0
                    for (i in 0 until ShopManager.items.size) {
                        if (py in itemY..(itemY + 12.0) && px in 80.0..320.0) {
                            ShopManager.selectedIndex = i
                            return@onDown
                        }
                        itemY += 12.0
                    }

                    // Check buy action button tap
                    if (py in (itemY + 15.0)..(itemY + 38.0) && px in 90.0..310.0) {
                        if (ShopManager.buyCurrent()) {
                            GameAudio.playBuy()
                        }
                    }
                }
            }
        }

        // Pointer move / dragging to aim
        onMove { evt ->
            val pos = evt.currentPosLocal
            val px = pos.x
            val py = pos.y

            if (game.state == GameState.PLAYING) {
                updateAimAngle(px, py)
            }
        }

        // Pointer up / releasing to fire spell!
        onUp {
            if (game.state == GameState.PLAYING && touchAimActive) {
                touchAimActive = false
                if (game.castSpell()) {
                    GameAudio.playCast()
                }
            }
        }

        // Desktop keyboard controls
        keys {
            down(Key.SPACE) {
                if (game.state == GameState.PLAYING && game.resurrect()) {
                    GameAudio.playResurrect()
                } else if (game.state == GameState.INTRO && introCooldown <= 0.0) {
                    game.state = GameState.PLAYING
                    game.player.spriteName = "norman_arms_down"
                    game.dialogue.clear()
                    GameAudio.play()
                    GameAudio.useLevelSynths(game.level)
                } else if (game.state == GameState.WIN || game.state == GameState.LOSE) {
                    onLose()
                }
            }
            down(Key.P) {
                isPaused = !isPaused
            }
            down(Key.UP) {
                if (game.state == GameState.SHOPPING) ShopManager.selectIndex(-1)
            }
            down(Key.DOWN) {
                if (game.state == GameState.SHOPPING) ShopManager.selectIndex(1)
            }
            down(Key.ENTER) {
                if (game.state == GameState.SHOPPING && ShopManager.buyCurrent()) {
                    GameAudio.playBuy()
                } else if (game.state == GameState.INTRO && introCooldown <= 0.0) {
                    game.state = GameState.PLAYING
                    game.player.spriteName = "norman_arms_down"
                    game.dialogue.clear()
                    GameAudio.play()
                    GameAudio.useLevelSynths(game.level)
                } else if (game.state == GameState.WIN || game.state == GameState.LOSE) {
                    onLose()
                }
            }
        }
    }

    private fun updateAimAngle(screenX: Double, screenY: Double) {
        val sceneX = screenX
        val sceneY = renderer.sceneOriginY - screenY
        val p1 = game.player.center()
        game.spell.targetAngle = atan2(sceneY - p1.y, sceneX - p1.x)
    }
}
